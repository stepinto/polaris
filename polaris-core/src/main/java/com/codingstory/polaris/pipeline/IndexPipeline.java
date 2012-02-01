package com.codingstory.polaris.pipeline;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.SimpleIdGenerator;
import com.codingstory.polaris.SnappyUtils;
import com.codingstory.polaris.indexing.DirectoryTranverser;
import com.codingstory.polaris.indexing.IndexPathUtils;
import com.codingstory.polaris.parser.ClassType;
import com.codingstory.polaris.parser.FileHandle;
import com.codingstory.polaris.parser.FirstPassProcessor;
import com.codingstory.polaris.parser.ImportExtractor;
import com.codingstory.polaris.parser.SecondPassProcessor;
import com.codingstory.polaris.parser.SourceAnnotator;
import com.codingstory.polaris.parser.SourceFile;
import com.codingstory.polaris.parser.SymbolTable;
import com.codingstory.polaris.parser.TClassType;
import com.codingstory.polaris.parser.TFileHandle;
import com.codingstory.polaris.parser.TSourceFile;
import com.codingstory.polaris.parser.TTypeUsage;
import com.codingstory.polaris.parser.TypeUsage;
import com.codingstory.polaris.parser.Usage;
import com.codingstory.polaris.repo.GitUtils;
import com.codingstory.polaris.repo.Repository;
import com.codingstory.polaris.sourcedb.SourceDbWriter;
import com.codingstory.polaris.sourcedb.SourceDbWriterImpl;
import com.codingstory.polaris.typedb.TypeDbWriter;
import com.codingstory.polaris.typedb.TypeDbWriterImpl;
import com.codingstory.polaris.usagedb.UsageDbWriter;
import com.codingstory.polaris.usagedb.UsageDbWriterImpl;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.crunch.CrunchRuntimeException;
import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.crunch.MapFn;
import org.apache.crunch.PCollection;
import org.apache.crunch.PTable;
import org.apache.crunch.Pair;
import org.apache.crunch.Pipeline;
import org.apache.crunch.PipelineResult;
import org.apache.crunch.fn.IdentityFn;
import org.apache.crunch.impl.mr.MRPipeline;
import org.apache.crunch.io.At;
import org.apache.crunch.lib.Join;
import org.apache.crunch.types.PTableType;
import org.apache.crunch.types.PType;
import org.apache.crunch.types.writable.WritableTypeFamily;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

import static org.apache.crunch.types.PTypes.thrifts;
import static org.apache.crunch.types.writable.Writables.longs;
import static org.apache.crunch.types.writable.Writables.strings;
import static org.apache.crunch.types.writable.Writables.tableOf;

/** A series of MapReduce jobs transforming source files into things needed to be indexed. */
public class IndexPipeline implements Serializable {

    private static final Log LOG = LogFactory.getLog(IndexPipeline.class);
    private static final IdGenerator ID_GENERATOR = new SimpleIdGenerator(); // Just OK for local MR.
    private static final TSerializer SERIALIZER = new TSerializer(new TBinaryProtocol.Factory());
    private static final TDeserializer DESERIALIZER = new TDeserializer(new TBinaryProtocol.Factory());
    private static final WritableTypeFamily TYPE_FAMILY = WritableTypeFamily.getInstance();
    private static final PType<TFileHandle> T_FILE_HANDLE_PTYPE_COMPRESSED = thriftsCompressed(TFileHandle.class);
    private static final PType<TParsedFile> T_PARSED_FILE_PTYPE_COMPRESSED = thriftsCompressed(TParsedFile.class);
    private static final PType<TFileContent> T_FILE_CONTENT_PTYPE_COMPRESSED = thriftsCompressed(TFileContent.class);

    /*
    private static class SnappyInputMapFn extends MapFn<ByteBuffer, ByteBuffer> {
        @Override
        public ByteBuffer map(ByteBuffer compressed) {
            byte[] compressedBytes = new byte[compressed.limit() - compressed.position()];
            System.arraycopy(compressed.array(), compressed.position(), compressedBytes, 0, compressedBytes.length);
            return ByteBuffer.wrap(SnappyUtils.uncompress(compressedBytes));
        }
    }

    private static class SnappyOutputMapFn extends MapFn<ByteBuffer, ByteBuffer> {
        @Override
        public ByteBuffer map(ByteBuffer uncompressed) {
            byte[] uncompressedBytes = new byte[uncompressed.limit() - uncompressed.position()];
            System.arraycopy(uncompressed.array(), uncompressed.position(),
                    uncompressedBytes, 0, uncompressedBytes.length);
            return ByteBuffer.wrap(SnappyUtils.compress(uncompressedBytes));
        }
    }
    */

    // Copied from crunch since the original is private.
    private static class ThriftInputMapFn<T extends TBase> extends MapFn<ByteBuffer, T> {

        private final Class<T> clazz;
        private transient T instance;
        private transient TDeserializer deserializer;
        private transient byte[] bytes;

        public ThriftInputMapFn(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public void initialize() {
            this.instance = ReflectionUtils.newInstance(clazz, null);
            this.deserializer = new TDeserializer(new TBinaryProtocol.Factory());
            this.bytes = new byte[0];
        }

        @Override
        public T map(ByteBuffer bb) {
            T next = (T) instance.deepCopy();
            int len = bb.limit() - bb.position();
            if (len != bytes.length) {
                bytes = new byte[len];
            }
            System.arraycopy(bb.array(), bb.position(), bytes, 0, len);
            try {
                deserializer.deserialize(next, SnappyUtils.uncompress(bytes));
            } catch (TException e) {
                throw new CrunchRuntimeException(e);
            }
            return next;
        }
    }

    // Copied from crunch since the original is private.
    private static class ThriftOutputMapFn<T extends TBase> extends MapFn<T, ByteBuffer> {

        private transient TSerializer serializer;

        public ThriftOutputMapFn() {
        }

        @Override
        public void initialize() {
            this.serializer = new TSerializer(new TBinaryProtocol.Factory());
        }

        @Override
        public ByteBuffer map(T t) {
            try {
                return ByteBuffer.wrap(SnappyUtils.compress(serializer.serialize(t)));
            } catch (TException e) {
                throw new CrunchRuntimeException(e);
            }
        }
    }

    private final transient Configuration conf; // No need to access it from MR tasks.
    private final transient FileSystem fs;
    private List<Repository> repos = Lists.newArrayList();
    private List<File> dirs = Lists.newArrayList();
    private File workingDir;
    private File inputDir1;
    private File inputDir2;
    private File outputDir;
    private File indexDir;

    public IndexPipeline() {
        try {
            conf = new Configuration();
            conf.setInt("io.sort.mb", 64); // To fit into 256MB heap.
            fs = FileSystem.getLocal(conf);
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    public void addRepoBase(File repoBase) throws IOException {
        Preconditions.checkNotNull(repoBase);
        for (Repository repo : GitUtils.openRepoBase(repoBase)) {
            repos.add(repo);
        }
    }

    public void addProjectDirectory(File dir) {
        Preconditions.checkNotNull(dir);
        dirs.add(dir);
    }

    public void setIndexDirectory(File dir) {
        indexDir = Preconditions.checkNotNull(dir);
    }

    public void run() throws IOException {
        setUpInputAndOutputDirs();
        for (Repository repo : repos) {
            readRepo(repo);
        }
        for (File dir : dirs) {
            readProjectDir(dir);
        }

        Pipeline pipeline = new MRPipeline(IndexPipeline.class, "polaris-index-pipeline", conf);
        PCollection<TFileContent> fileContents = pipeline
            .read(At.sequenceFile(new Path(inputDir1.getPath()), T_FILE_CONTENT_PTYPE_COMPRESSED));
        PCollection<TParsedFile> parsedFiles1stPass = discoverClasses(fileContents);
        PCollection<TFileImports> fileImports = extractImports(fileContents);
        PTable<TFileHandle, TFileHandle> importGraph1 =
                guessImportGraphByImportedClasses(fileImports, parsedFiles1stPass);
        PTable<TFileHandle, TFileHandle> importGraph2 =
                guessImportGraphBySamePackage(parsedFiles1stPass);
        PTable<TFileHandle, TFileHandle> importGraph = importGraph1.union(importGraph2); // TODO: remove duplication
        // TODO: reverseImportGraphByImportedPackage
        // TODO: reverseImportGraphByFilePackage
        // TODO: Iterate to compute transitive closure
        PCollection<TParsedFile> parsedFiles2ndPass = discoverMembers(parsedFiles1stPass, importGraph);
        pipeline.write(parsedFiles2ndPass, At.sequenceFile(new Path(outputDir.getPath()), T_PARSED_FILE_PTYPE_COMPRESSED));

        LOG.info("About to run indexing pipeline");
        checkPipelineResult(pipeline.run());
        LOG.info("Pipeline completes");

        buildIndexFromPipelineOutput();
    }

    private void checkPipelineResult(PipelineResult result) throws IOException {
        if (!result.succeeded()) {
            throw new IOException("Pipeline failed");
        }
    }

    private PCollection<TFileImports> extractImports(PCollection<TFileContent> fileContents) {
        return fileContents.parallelDo(new DoFn<TFileContent, TFileImports>() {
                @Override
                public void process(TFileContent in, Emitter<TFileImports> emitter) {
                    try {
                        ImportExtractor.Result result = ImportExtractor.findImports(
                                new ByteArrayInputStream(in.getContent().getBytes()));
                        TFileImports out = new TFileImports();
                        out.setFile(in.getFile());
                        out.setPackage_(result.getPackage());
                        out.setImportedClasses(result.getImportedClasses());
                        out.setImportedPackages(result.getImportedPackages());

                    } catch (IOException e) {
                        LOG.warn("Failed to parse " + FileHandle.createFromThrift(in.getFile()));
                        LOG.debug("Exception", e);
                    }
                }
            }, thrifts(TFileImports.class, TYPE_FAMILY));
    }

    private PCollection<TParsedFile> discoverClasses(PCollection<TFileContent> fileContents) {
        return fileContents.parallelDo(new DoFn<TFileContent, TParsedFile>() {
            @Override
            public void process(TFileContent in, Emitter<TParsedFile> emitter) {
                try {
                    FirstPassProcessor.Result result = FirstPassProcessor.process(
                            FileHandle.createFromThrift(in.getFile()),
                            new ByteArrayInputStream(in.getContent().getBytes()),
                            ID_GENERATOR,
                            new SymbolTable());
                    TSourceFile sourceFile = new TSourceFile();
                    sourceFile.setHandle(in.getFile());
                    sourceFile.setSource(in.getContent());
                    TParsedFile out = new TParsedFile();
                    out.setSource(sourceFile);
                    out.setPackage_(result.getPackage());
                    for (ClassType classType : result.getDiscoveredClasses()) {
                        out.addToClasses(classType.toThrift());
                    }
                    emitter.emit(out);
                } catch (IOException e) {
                    LOG.warn("Failed to parse " + FileHandle.createFromThrift(in.getFile()));
                    LOG.debug("Exception", e);
                }
            }
        }, thrifts(TParsedFile.class, TYPE_FAMILY));
    }

    private void setUpInputAndOutputDirs() throws IOException {
        workingDir = Files.createTempDir();
        inputDir1 = new File(workingDir, "in1");
        inputDir2 = new File(workingDir, "in2");
        outputDir = new File(workingDir, "out");
        FileUtils.forceMkdir(inputDir1);
        FileUtils.forceMkdir(inputDir2);
        FileUtils.forceMkdir(outputDir);
        conf.set("hadoop.tmp.dir", workingDir.getPath());
        LOG.info("Temporary working dirctory: " + workingDir);
    }

    private void readRepo(Repository repo) throws IOException {
        Preconditions.checkNotNull(repo);
        LOG.info("Scanning repository: " + repo.getName());
        File workingTree = checkOutWorkTree(repo);
        doReadProjectDir(repo.getName(), workingTree);
        FileUtils.deleteDirectory(workingTree);
    }

    private void readProjectDir(File dir) throws IOException {
        LOG.info("Scanning project root: " + dir.getName());
        doReadProjectDir(dir.getName(), dir);
    }

    private void doReadProjectDir(String project, File dir) throws IOException {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(dir);
        try {
            SequenceFile.Writer w1 = SequenceFile.createWriter(fs, conf,
                    new Path(new File(inputDir1, "data").getPath()), NullWritable.class, BytesWritable.class);
            SequenceFile.Writer w2 = SequenceFile.createWriter(fs, conf,
                    new Path(new File(inputDir2, "data").getPath()), NullWritable.class, BytesWritable.class);

            final List<File> sourceDirs = Lists.newArrayList();
            final List<File> sourceFiles = Lists.newArrayList();
            DirectoryTranverser.traverse(dir, new DirectoryTranverser.Visitor() {
                @Override
                public void visit(File file) {
                    if (file.isHidden()) {
                        return;
                    }
                    if (file.isDirectory()) {
                        sourceDirs.add(file);
                    } else if (file.getName().endsWith(".java")) {
                        sourceFiles.add(file);
                    }
                }
            });

            LOG.info("Found " + sourceFiles.size() + " file(s)");
            long count = 0;
            for (File sourceFile : sourceFiles) {
                TFileContent in = new TFileContent();
                FileHandle handle = new FileHandle(ID_GENERATOR.next(), project,
                        StringUtils.removeStart(sourceFile.getPath(), dir.getPath()));
                in.setFile(handle.toThrift());
                in.setContent(FileUtils.readFileToString(sourceFile));
                w1.append(NullWritable.get(), new BytesWritable(SnappyUtils.compress(SERIALIZER.serialize(in))));
                count++;
                if (count % 5000 == 0) {
                    LOG.info("Processed " + count + " files");
                }
            }
            w1.close();

            for (File sourceDir : sourceDirs) {
                TFileHandle f = new TFileHandle();
                f.setId(ID_GENERATOR.next());
                f.setProject(project);
                f.setPath(StringUtils.removeStart(sourceDir.getPath(), dir.getPath()) + "/");
                w2.append(NullWritable.get(), new BytesWritable(SnappyUtils.compress(SERIALIZER.serialize(f))));
            }
            w2.close();
        } catch (TException e) {
            throw new AssertionError(e);
        }
    }

    private File checkOutWorkTree(Repository repo) throws IOException {
        File tempDir = Files.createTempDir();
        GitUtils.checkoutWorkTree(repo, tempDir);
        return tempDir;
    }

    private PTable<TFileHandle, TFileHandle> guessImportGraphByImportedClasses(
            PCollection<TFileImports> fileImports,
            PCollection<TParsedFile> discoveredClassesPerFile) {
        PTable<String, TFileHandle> left = fileImports.parallelDo(new DoFn<TFileImports, Pair<String, TFileHandle>>() {
            @Override
            public void process(TFileImports in, Emitter<Pair<String, TFileHandle>> emitter) {
                if (!in.isSetImportedClasses()) {
                    return;
                }
                for (String clazz : in.getImportedClasses()) {
                    emitter.emit(Pair.of(clazz, in.getFile()));
                }
            }
        }, tableOf(strings(), T_FILE_HANDLE_PTYPE_COMPRESSED));

        PTable<String, TFileHandle> right = discoveredClassesPerFile.parallelDo(
                new DoFn<TParsedFile, Pair<String, TFileHandle>>() {
                    @Override
                    public void process(
                            TParsedFile in,
                            Emitter<Pair<String, TFileHandle>> emitter) {
                        if (!in.isSetClasses()) {
                            return;
                        }
                        for (TClassType clazz : in.getClasses()) {
                            emitter.emit(Pair.of(clazz.getHandle().getName(), in.getSource().getHandle()));
                        }
                    }
                }, tableOf(strings(), T_FILE_HANDLE_PTYPE_COMPRESSED));

        return left.join(right).values().parallelDo(
                new MapFn<Pair<TFileHandle, TFileHandle>, Pair<TFileHandle, TFileHandle>>() {
                    @Override
                    public Pair<TFileHandle, TFileHandle> map(Pair<TFileHandle, TFileHandle> in) {
                        return in;
                    }
                },tableOf(T_FILE_HANDLE_PTYPE_COMPRESSED, T_FILE_HANDLE_PTYPE_COMPRESSED));
    }

    /** Produces import relation A -> B if A and B are in same package. */
    private PTable<TFileHandle, TFileHandle> guessImportGraphBySamePackage(PCollection<TParsedFile> parsedFiles) {
        PTable<String, TParsedFile> parsedFilesByPackage = pivotParsedFilesByPackage(parsedFiles);
        return Join.innerJoin(parsedFilesByPackage, parsedFilesByPackage).values().parallelDo(
                new MapFn<Pair<TParsedFile, TParsedFile>, Pair<TFileHandle, TFileHandle>>() {
                    @Override
                    public Pair<TFileHandle, TFileHandle> map(Pair<TParsedFile, TParsedFile> in) {
                        return Pair.of(in.first().getSource().getHandle(), in.second().getSource().getHandle());
                    }
                }, tableOf(T_FILE_HANDLE_PTYPE_COMPRESSED, T_FILE_HANDLE_PTYPE_COMPRESSED));
    }

    private PTable<TFileHandle, TFileHandle> reverseImportGraph(PTable<TFileHandle, TFileHandle> importGraph) {
        return importGraph.parallelDo(new MapFn<Pair<TFileHandle, TFileHandle>, Pair<TFileHandle, TFileHandle>>() {
            @Override
            public Pair<TFileHandle, TFileHandle> map(Pair<TFileHandle, TFileHandle> in) {
                return Pair.of(in.second(), in.first());
            }
        }, tableOf(T_FILE_HANDLE_PTYPE_COMPRESSED, T_FILE_HANDLE_PTYPE_COMPRESSED));
    }

    private PCollection<TParsedFile> discoverMembers(
            PCollection<TParsedFile> discoveredClassesPerFile,
            PTable<TFileHandle, TFileHandle> importGraph) {
        PTable<Long, TParsedFile> parsedFilesById =
                pivotDiscoveredClassByFileId(discoveredClassesPerFile);
        PTable<Long, Long> importGraphPlainIds = plainFileIdsFromImportGraph(importGraph);
        PTable<Long, Long> inverseImportGraphPlainIds = inverse(importGraphPlainIds, tableOf(longs(), longs()));
        PTable<Long, TParsedFile> parsedFilesByImporterId = inverseImportGraphPlainIds.join(
                parsedFilesById).values().parallelDo(
                        IdentityFn.<Pair<Long, TParsedFile>>getInstance(),
                        tableOf(longs(), T_PARSED_FILE_PTYPE_COMPRESSED));
        // Left join because a file can be imported by nobody.
        return Join.leftJoin(parsedFilesById, parsedFilesByImporterId.collectValues()).values()
                .parallelDo(new MapFn<Pair<TParsedFile, Collection<TParsedFile>>, TParsedFile>() {
                    @Override
                    public TParsedFile map(Pair<TParsedFile, Collection<TParsedFile>> in) {
                        try {
                            TParsedFile currentFile = in.first();
                            TFileHandle fileHandle = currentFile.getSource().getHandle();
                            Collection<TParsedFile> importedFiles = in.second();
                            List<TParsedFile> importedFilesPlusSelf = Lists.newArrayList();
                            importedFilesPlusSelf.add(currentFile);
                            if (importedFiles != null) {
                                importedFilesPlusSelf.addAll(importedFiles);
                            }
                            SymbolTable symbolTable = new SymbolTable();
                            for (TParsedFile t : importedFilesPlusSelf) {
                                if (t.isSetClasses()) {
                                    for (TClassType tclazz : t.getClasses()) {
                                        ClassType clazz = ClassType.createFromThrift(tclazz);
                                        symbolTable.registerClassType(clazz);
                                    }
                                }
                            }
                            String content = currentFile.getSource().getSource();
                            SecondPassProcessor.Result result = SecondPassProcessor.extract(
                                    fileHandle.getProject(),
                                    FileHandle.createFromThrift(fileHandle),
                                    new ByteArrayInputStream(content.getBytes()),
                                    symbolTable,
                                    ID_GENERATOR,
                                    currentFile.getPackage_());
                            for (ClassType clazz : result.getClassTypes()) {
                                currentFile.getClasses().clear();
                                currentFile.addToClasses(clazz.toThrift());
                            }
                            for (Usage usage : result.getUsages()) {
                                if (usage instanceof TypeUsage) {
                                    // TODO: We only support TypeUsage now.
                                    currentFile.addToTypeUsages(((TypeUsage) usage).toThrift());
                                }
                            }
                            String annotated = SourceAnnotator.annotate(
                                    new ByteArrayInputStream(content.getBytes()),
                                    result.getUsages());
                            currentFile.getSource().setAnnotatedSource(annotated);
                            return currentFile;
                        } catch (IOException e) {
                            // Since we've inner-joined "parsedFilesById", no exceptions should occur.
                            throw new AssertionError(e);
                        }
                    }
                }, T_PARSED_FILE_PTYPE_COMPRESSED);
    }

    private PTable<Long, TParsedFile> pivotDiscoveredClassByFileId(PCollection<TParsedFile> parsedFiles) {
        return parsedFiles.parallelDo(
                new MapFn<TParsedFile, Pair<Long, TParsedFile>>() {
            @Override
            public Pair<Long, TParsedFile> map(TParsedFile in) {
                return Pair.of(in.getSource().getHandle().getId(), in);
            }
        }, tableOf(longs(), T_PARSED_FILE_PTYPE_COMPRESSED));
    }

    private PTable<String,TParsedFile> pivotParsedFilesByPackage(PCollection<TParsedFile> parsedFiles) {
        return parsedFiles.by(new MapFn<TParsedFile, String>() {
            @Override
            public String map(TParsedFile in) {
                return in.getPackage_();
            }
        }, strings());
    }

    private static PTable<Long, TFileContent> pivotFileContentById(PCollection<TFileContent> fileContents) {
        return fileContents.parallelDo(new MapFn<TFileContent, Pair<Long, TFileContent>>() {
            @Override
            public Pair<Long, TFileContent> map(TFileContent in) {
                return Pair.of(in.getFile().getId(), in);
            }
        }, tableOf(longs(), T_FILE_CONTENT_PTYPE_COMPRESSED));
    }

    private static PTable<Long, Long> plainFileIdsFromImportGraph(PTable<TFileHandle, TFileHandle> importGraph) {
        return importGraph.parallelDo(new MapFn<Pair<TFileHandle, TFileHandle>, Pair<Long, Long>>() {
            @Override
            public Pair<Long, Long> map(Pair<TFileHandle, TFileHandle> in) {
                return Pair.of(in.first().getId(), in.second().getId());
            }
        }, tableOf(longs(), longs()));
    }

    private static <K, V> PTable<V, K> inverse(PTable<K, V> table, PTableType<V, K> ptype) {
        return table.parallelDo(new MapFn<Pair<K, V>, Pair<V, K>>() {
            @Override
            public Pair<V, K> map(Pair<K, V> in) {
                return Pair.of(in.second(), in.first());
            }
        }, ptype);
    }

    private void buildIndexFromPipelineOutput() throws IOException {
        // TODO: Build index in pipeline.
        TypeDbWriter typeDb = null;
        SourceDbWriter sourceDb = null;
        UsageDbWriter usageDb = null;
        try {
            typeDb = new TypeDbWriterImpl(IndexPathUtils.getTypeDbPath(indexDir));
            sourceDb = new SourceDbWriterImpl(IndexPathUtils.getSourceDbPath(indexDir));
            usageDb = new UsageDbWriterImpl(IndexPathUtils.getUsageDbPath(indexDir));

            // Process pipeline output.
            for (File file : outputDir.listFiles()) {
                if (file.getPath().endsWith(".crc")) {
                    continue;
                }
                LOG.info("Build index from pipeline output: " + file);
                SequenceFile.Reader r = new SequenceFile.Reader(fs, new Path(file.getPath()), conf);
                BytesWritable value = new BytesWritable();
                while (r.next(NullWritable.get(), value)) {
                    TParsedFile parsedFile = new TParsedFile();
                    DESERIALIZER.deserialize(
                            parsedFile,
                            SnappyUtils.uncompress(value.getBytes(), 0, value.getLength()));
                    if (parsedFile.isSetClasses()) {
                        for (TClassType t : parsedFile.getClasses()) {
                            typeDb.write(ClassType.createFromThrift(t));
                        }
                    }
                    if (parsedFile.isSetSource()) {
                        sourceDb.writeSourceFile(SourceFile.createFromThrift(parsedFile.getSource()));
                    }
                    if (parsedFile.isSetTypeUsages()) {
                        for (TTypeUsage t : parsedFile.getTypeUsages()) {
                            TypeUsage typeUsage = TypeUsage.createFromThrift(t);
                            if (typeUsage.getType().isResolved()) {
                                usageDb.write(TypeUsage.createFromThrift(t));
                            }
                        }
                    }
                }
            }

            // Process repository layout.
            {
                SequenceFile.Reader r = new SequenceFile.Reader(
                        fs, new Path(new File(inputDir2, "data").getPath()), conf);
                BytesWritable value = new BytesWritable();
                while (r.next(NullWritable.get(), value)) {
                    TFileHandle f = new TFileHandle();
                    DESERIALIZER.deserialize(f, SnappyUtils.uncompress(value.getBytes(), 0, value.getLength()));
                    sourceDb.writeDirectory(f.getProject(), f.getPath());
                }
            }

            LOG.info("Index files are written to " + indexDir);
        } catch (TException e) {
            throw new AssertionError(e);
        } finally {
            IOUtils.closeQuietly(typeDb);
            IOUtils.closeQuietly(sourceDb);
            IOUtils.closeQuietly(usageDb);
        }
    }

    public void cleanUp() {
        LOG.info("Deleting temporary working directory: " + workingDir);
        FileUtils.deleteQuietly(workingDir);
    }

    private static <T extends TBase> PType<T> thriftsCompressed(Class<T> clazz) {
        return TYPE_FAMILY.derived(
                clazz,
                new ThriftInputMapFn<T>(clazz),
                new ThriftOutputMapFn<T>(),
                TYPE_FAMILY.bytes());
    }
}
