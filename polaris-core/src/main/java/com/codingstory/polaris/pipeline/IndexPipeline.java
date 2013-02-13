package com.codingstory.polaris.pipeline;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.SimpleIdGenerator;
import com.codingstory.polaris.indexing.DirectoryTranverser;
import com.codingstory.polaris.indexing.IndexPathUtils;
import com.codingstory.polaris.parser.FirstPassProcessor;
import com.codingstory.polaris.parser.ImportExtractor;
import com.codingstory.polaris.parser.ParserProtos;
import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.ClassTypeHandle;
import com.codingstory.polaris.parser.ParserProtos.FileHandle;
import com.codingstory.polaris.parser.ParserProtos.SourceFile;
import com.codingstory.polaris.parser.ParserProtos.TypeHandle;
import com.codingstory.polaris.parser.ParserProtos.TypeUsage;
import com.codingstory.polaris.parser.ParserProtos.Usage;
import com.codingstory.polaris.parser.SecondPassProcessor;
import com.codingstory.polaris.parser.SourceAnnotator;
import com.codingstory.polaris.parser.SymbolTable;
import com.codingstory.polaris.pipeline.PipelineProtos.FileContent;
import com.codingstory.polaris.pipeline.PipelineProtos.FileImports;
import com.codingstory.polaris.pipeline.PipelineProtos.ParsedFile;
import com.codingstory.polaris.repo.GitUtils;
import com.codingstory.polaris.repo.Repository;
import com.codingstory.polaris.sourcedb.SourceDbWriter;
import com.codingstory.polaris.sourcedb.SourceDbWriterImpl;
import com.codingstory.polaris.typedb.TypeDbWriter;
import com.codingstory.polaris.typedb.TypeDbWriterImpl;
import com.codingstory.polaris.usagedb.UsageDbWriter;
import com.codingstory.polaris.usagedb.UsageDbWriterImpl;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.crunch.MapFn;
import org.apache.crunch.PCollection;
import org.apache.crunch.PTable;
import org.apache.crunch.Pair;
import org.apache.crunch.PipelineResult;
import org.apache.crunch.fn.IdentityFn;
import org.apache.crunch.impl.mr.MRPipeline;
import org.apache.crunch.impl.mr.plan.PlanningParameters;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.apache.crunch.types.PTypes.protos;
import static org.apache.crunch.types.writable.Writables.longs;
import static org.apache.crunch.types.writable.Writables.strings;
import static org.apache.crunch.types.writable.Writables.tableOf;

/** A series of MapReduce jobs transforming source files into things needed to be indexed. */
public class IndexPipeline implements Serializable {

    private static final Log LOG = LogFactory.getLog(IndexPipeline.class);
    private static final IdGenerator ID_GENERATOR = new SimpleIdGenerator(); // Just OK for local MR.
    private static final WritableTypeFamily TYPE_FAMILY = WritableTypeFamily.getInstance();
    private static final PType<ParsedFile> PARSED_FILE_PTYPE = protos(ParsedFile.class, TYPE_FAMILY);
    private static final PType<FileContent> FILE_CONTENT_PTYPE = protos(FileContent.class, TYPE_FAMILY);
    private static final PType<FileImports> FILE_IMPORTS_PTYPE = protos(FileImports.class, TYPE_FAMILY);

    private final transient Configuration conf; // "transient" No need to access it from MR tasks.
    private final transient FileSystem fs;
    private transient List<Repository> repos = Lists.newArrayList();
    private transient List<File> dirs = Lists.newArrayList();
    private File workingDir;
    private File inputDir1;
    private File inputDir2;
    private File outputDir;
    private File indexDir;

    public IndexPipeline() {
        try {
            conf = new Configuration();
            conf.setInt("io.sort.mb", 32); // To fit into 256MB heap.
            conf.setBoolean("mapreduce.map.output.compress", true);
            conf.setBoolean("mapreduce.output.fileoutputformat.compress", true);
            conf.setStrings("mapreduce.output.fileoutputformat.compress.type", "BLOCK");
            conf.setBoolean("crunch.log.job.progress", true);
            SequenceFile.setDefaultCompressionType(conf, SequenceFile.CompressionType.BLOCK);
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

        MRPipeline pipeline = setUpPipeline();
        LOG.info("About to run indexing pipeline");
        checkPipelineResult(pipeline.run());
        LOG.info("Pipeline completes");

        buildIndexFromPipelineOutput();
    }

    public String plan() throws IOException {
        setUpInputAndOutputDirs();
        MRPipeline pipeline = setUpPipeline();
        pipeline.plan();
        return conf.get(PlanningParameters.PIPELINE_PLAN_DOTFILE);
    }

    private MRPipeline setUpPipeline() {
        MRPipeline pipeline = new MRPipeline(IndexPipeline.class, "polaris-index-pipeline", conf);
        pipeline.enableDebug();
        PCollection<FileContent> fileContents = pipeline
            .read(At.sequenceFile(new Path(inputDir1.getPath()), FILE_CONTENT_PTYPE));
        PCollection<ParsedFile> parsedFiles1stPass = discoverClasses(fileContents);
        PCollection<FileImports> fileImports = extractImports(fileContents);
        PTable<Long, Long> importGraph1 =
                guessImportGraphByImportedClasses(fileImports, parsedFiles1stPass);
        PTable<Long, Long> importGraph2 =
                guessImportGraphBySamePackage(parsedFiles1stPass);
        PTable<Long, Long> importGraph = importGraph1.union(importGraph2); // TODO: remove duplication
        // TODO: reverseImportGraphByImportedPackage
        // TODO: reverseImportGraphByFilePackage
        // TODO: Iterate to compute transitive closure
        PCollection<ParsedFile> parsedFiles2ndPass = discoverMembers(fileContents, parsedFiles1stPass, importGraph);
        pipeline.write(parsedFiles2ndPass, At.sequenceFile(new Path(outputDir.getPath()), PARSED_FILE_PTYPE));
        return pipeline;
    }

    private void checkPipelineResult(PipelineResult result) throws IOException {
        if (!result.succeeded()) {
            throw new IOException("Pipeline failed");
        }
    }

    private PCollection<FileImports> extractImports(PCollection<FileContent> fileContents) {
        return fileContents.parallelDo("ExtractImports", new DoFn<FileContent, FileImports>() {
            @Override
            public void process(FileContent in, Emitter<FileImports> emitter) {
                try {
                    ImportExtractor.Result result = ImportExtractor.findImports(
                            new ByteArrayInputStream(in.getContent().getBytes()));
                    emitter.emit(FileImports.newBuilder()
                            .setFile(in.getFile())
                            .setPackage(result.getPackage())
                            .addAllImportedClasses(result.getImportedClasses())
                            .addAllImportedPackages(result.getImportedPackages())
                            .build());
                } catch (IOException e) {
                    LOG.warn("Failed to parse " + in.getFile());
                    LOG.debug("Exception", e);
                }
            }
        }, FILE_IMPORTS_PTYPE);
    }

    private PCollection<ParsedFile> discoverClasses(PCollection<FileContent> fileContents) {
        PCollection<ParsedFile> parsedFiles =
                fileContents.parallelDo("FirstPass", new DoFn<FileContent, ParsedFile>() {
                    @Override
                    public void process(FileContent in, Emitter<ParsedFile> emitter) {
                        try {
                            FirstPassProcessor.Result result = FirstPassProcessor.process(
                                    in.getFile(),
                                    new ByteArrayInputStream(in.getContent().getBytes()),
                                    ID_GENERATOR,
                                    new SymbolTable());
                            SourceFile sourceFile = SourceFile.newBuilder()
                                    .setHandle(in.getFile())
                                    .build();
                            // Don't save file content for now, since ParsedFile produced by 1st pass is
                            // joined and duplicated for many times (= number of references).
                            ParsedFile out = ParsedFile.newBuilder()
                                    .setSource(sourceFile)
                                    .setPackage(result.getPackage())
                                    .addAllClasses(result.getDiscoveredClasses())
                                    .build();
                            emitter.emit(out);
                        } catch (IOException e) {
                            LOG.warn("Failed to parse " + in.getFile());
                            LOG.debug("Exception", e);
                        }
                    }
                }, PARSED_FILE_PTYPE);

        // Force first pass is executed once.
        return pivotParsedFilesByFileId(parsedFiles).collectValues().values().parallelDo(
                new DoFn<Collection<ParsedFile>, ParsedFile>() {
                    @Override
                    public void process(Collection<ParsedFile> in, Emitter<ParsedFile> emitter) {
                        for (ParsedFile t : in) {
                            emitter.emit(t);
                        }
                    }
                }, PARSED_FILE_PTYPE);
    }

    private void setUpInputAndOutputDirs() throws IOException {
        workingDir = File.createTempFile("polaris-pipeline-", "");
        FileUtils.deleteQuietly(workingDir);
        FileUtils.forceMkdir(workingDir);
        inputDir1 = new File(workingDir, "in1");
        inputDir2 = new File(workingDir, "in2");
        outputDir = new File(workingDir, "out");
        FileUtils.forceMkdir(inputDir1);
        FileUtils.forceMkdir(inputDir2);
        conf.set("hadoop.tmp.dir", workingDir.getPath());
        conf.set("crunch.tmp.dir", workingDir.getPath());
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
        SequenceFile.Writer w1 = SequenceFile.createWriter(fs, conf,
                new Path(new File(inputDir1, "sources-of-" + project).getPath()),
                NullWritable.class, BytesWritable.class);
        SequenceFile.Writer w2 = SequenceFile.createWriter(fs, conf,
                new Path(new File(inputDir2, "dirs-of-" + project).getPath()),
                NullWritable.class, BytesWritable.class);

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
            FileHandle handle = FileHandle.newBuilder()
                    .setId(ID_GENERATOR.next())
                    .setProject(project)
                    .setPath(StringUtils.removeStart(sourceFile.getPath(), dir.getPath()))
                    .build();
            FileContent in = FileContent.newBuilder()
                    .setFile(handle)
                    .setContent(FileUtils.readFileToString(sourceFile))
                    .build();
            w1.append(NullWritable.get(), new BytesWritable(in.toByteArray()));
            count++;
            if (count % 5000 == 0) {
                LOG.info("Processed " + count + " files");
            }
        }
        w1.close();

        for (File sourceDir : sourceDirs) {
            FileHandle f = FileHandle.newBuilder()
                    .setId(ID_GENERATOR.next())
                    .setProject(project)
                    .setPath(StringUtils.removeStart(sourceDir.getPath(), dir.getPath()) + "/")
                    .build();
            w2.append(NullWritable.get(), new BytesWritable(f.toByteArray()));
        }
        w2.close();
    }

    private File checkOutWorkTree(Repository repo) throws IOException {
        File tempDir = Files.createTempDir();
        GitUtils.checkoutWorkTree(repo, tempDir);
        return tempDir;
    }

    private PTable<Long, Long> guessImportGraphByImportedClasses(
            PCollection<FileImports> fileImports,
            PCollection<ParsedFile> parsedFiles) {
        PTable<String, Long> left = fileImports.parallelDo(
                "ParsedFilesByImportedClasses",
                new DoFn<FileImports, Pair<String, Long>>() {
                    @Override
                    public void process(FileImports in, Emitter<Pair<String, Long>> emitter) {
                        for (String clazz : in.getImportedClassesList()) {
                            emitter.emit(Pair.of(clazz, in.getFile().getId()));
                        }
                    }
                }, tableOf(strings(), longs()));

        PTable<String, Long> right = parsedFiles.parallelDo(
                "ParsedFilesByDeclaredClasses",
                new DoFn<ParsedFile, Pair<String, Long>>() {
                    @Override
                    public void process(
                            ParsedFile in,
                            Emitter<Pair<String, Long>> emitter) {
                        for (ClassType clazz : in.getClassesList()) {
                            emitter.emit(Pair.of(clazz.getHandle().getName(), in.getSource().getHandle().getId()));
                        }
                    }
                }, tableOf(strings(), longs()));

        return left.join(right).values().parallelDo(
                IdentityFn.<Pair<Long, Long>>getInstance(), tableOf(longs(), longs()));
    }

    /** Produces import relation A -> B if A and B are in same package. */
    private PTable<Long, Long> guessImportGraphBySamePackage(PCollection<ParsedFile> parsedFiles) {
        PTable<String, ParsedFile> parsedFilesByPackage = pivotParsedFilesByPackage(parsedFiles);
        return Join.innerJoin(parsedFilesByPackage, parsedFilesByPackage).values().parallelDo(
                new MapFn<Pair<ParsedFile, ParsedFile>, Pair<Long, Long>>() {
                    @Override
                    public Pair<Long, Long> map(Pair<ParsedFile, ParsedFile> in) {
                        return Pair.of(
                                in.first().getSource().getHandle().getId(),
                                in.second().getSource().getHandle().getId());
                    }
                }, tableOf(longs(), longs()));
    }

    private PCollection<ParsedFile> discoverMembers(
            PCollection<FileContent> fileContents,
            PCollection<ParsedFile> parsedFiles,
            PTable<Long, Long> importGraph) {
        // Assume A imports B...
        PTable<Long, ParsedFile> parsedFilesById = pivotParsedFilesByFileId(
                fillFileContents(parsedFiles, fileContents)); // A -> class A {...}
        PTable<Long, Long> invertImportGraph = inverse(importGraph, tableOf(longs(), longs())); // B -> A
        PTable<Long, ParsedFile> parsedFilesByImporterId = invertImportGraph.join(
                parsedFilesById).values().parallelDo( // A -> class B {...}
                        IdentityFn.<Pair<Long, ParsedFile>>getInstance(),
                        tableOf(longs(), PARSED_FILE_PTYPE));

        // Left join because a file can be imported by nobody.
        return Join.leftJoin(parsedFilesById, parsedFilesByImporterId.collectValues()).values()
                .parallelDo("SecondPass", new MapFn<Pair<ParsedFile, Collection<ParsedFile>>, ParsedFile>() {
                    @Override
                    public ParsedFile map(Pair<ParsedFile, Collection<ParsedFile>> in) {
                        try {
                            ParsedFile currentFile = in.first();
                            FileHandle fileHandle = currentFile.getSource().getHandle();
                            Collection<ParsedFile> importedFiles = in.second();
                            List<ParsedFile> importedFilesPlusSelf = Lists.newArrayList();
                            importedFilesPlusSelf.add(currentFile);
                            if (importedFiles != null) {
                                importedFilesPlusSelf.addAll(importedFiles);
                            }
                            SymbolTable symbolTable = new SymbolTable();
                            for (ParsedFile t : importedFilesPlusSelf) {
                                for (ClassType clazz : t.getClassesList()) {
                                    symbolTable.registerClassType(clazz);
                                }
                            }
                            String content = currentFile.getSource().getSource();
                            SecondPassProcessor.Result result = SecondPassProcessor.extract(
                                    fileHandle.getProject(),
                                    fileHandle,
                                    new ByteArrayInputStream(content.getBytes()),
                                    symbolTable,
                                    ID_GENERATOR,
                                    currentFile.getPackage());
                            String annotated = SourceAnnotator.annotate(
                                    new ByteArrayInputStream(content.getBytes()),
                                    result.getUsages());
                            return ParsedFile.newBuilder()
                                    .setPackage(currentFile.getPackage())
                                    .addAllClasses(result.getClassTypes())
                                    .addAllUsages(result.getUsages())
                                    .setSource(currentFile.getSource().toBuilder().setAnnotatedSource(annotated).build())
                                    .build();
                        } catch (IOException e) {
                            // Since we've inner-joined "parsedFilesById", no exceptions should occur.
                            throw new AssertionError(e);
                        }
                    }
                }, PARSED_FILE_PTYPE);
    }

    private PTable<Long, ParsedFile> pivotParsedFilesByFileId(PCollection<ParsedFile> parsedFiles) {
        return parsedFiles.parallelDo(
                "ParsedFilesByFileId",
                new MapFn<ParsedFile, Pair<Long, ParsedFile>>() {
                    @Override
                    public Pair<Long, ParsedFile> map(ParsedFile in) {
                        return Pair.of(in.getSource().getHandle().getId(), in);
                    }
                }, tableOf(longs(), PARSED_FILE_PTYPE));
    }

    private PTable<String,ParsedFile> pivotParsedFilesByPackage(PCollection<ParsedFile> parsedFiles) {
        return parsedFiles.by("ParsedFilesByPackage", new MapFn<ParsedFile, String>() {
            @Override
            public String map(ParsedFile in) {
                return in.getPackage();
            }
        }, strings());
    }

    private static PTable<Long, FileContent> pivotFileContentById(PCollection<FileContent> fileContents) {
        return fileContents.parallelDo("FileContentsById", new MapFn<FileContent, Pair<Long, FileContent>>() {
            @Override
            public Pair<Long, FileContent> map(FileContent in) {
                return Pair.of(in.getFile().getId(), in);
            }
        }, tableOf(longs(), FILE_CONTENT_PTYPE));
    }

    private static <K, V> PTable<V, K> inverse(PTable<K, V> table, PTableType<V, K> ptype) {
        return table.parallelDo("InverseTable", new MapFn<Pair<K, V>, Pair<V, K>>() {
            @Override
            public Pair<V, K> map(Pair<K, V> in) {
                return Pair.of(in.second(), in.first());
            }
        }, ptype);
    }

    private PCollection<ParsedFile> fillFileContents(
            PCollection<ParsedFile> parsedFiles,
            PCollection<FileContent> fileContents) {
        PTable<Long, ParsedFile> left = pivotParsedFilesByFileId(parsedFiles);
        PTable<Long, FileContent> right = pivotFileContentById(fileContents);
        return left.join(right).values().parallelDo(
                "JoinParsedFilesAndFileContents",
                new MapFn<Pair<ParsedFile, FileContent>, ParsedFile>() {
                    @Override
                    public ParsedFile map(Pair<ParsedFile, FileContent> in) {
                        ParsedFile parsedFile = in.first();
                        FileContent fileContent = in.second();
                        SourceFile source = parsedFile.getSource()
                                .toBuilder()
                                .setSource(fileContent.getContent())
                                .build();
                        return parsedFile.toBuilder()
                                .setSource(source)
                                .build();
                    }
                }, PARSED_FILE_PTYPE);
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
                    ParsedFile parsedFile = ParsedFile.parseFrom(Arrays.copyOf(value.getBytes(), value.getLength()));
                    for (ClassType clazz : parsedFile.getClassesList()) {
                        typeDb.write(clazz);
                    }
                    if (parsedFile.hasSource()) {
                        sourceDb.writeSourceFile(parsedFile.getSource());
                    }
                    for (Usage u : parsedFile.getUsagesList()) {
                        // TODO: save all kinds of usages
                        if (Objects.equal(u.getKind(), Usage.Kind.TYPE)) {
                            TypeUsage typeUsage = u.getType();
                            TypeHandle type = typeUsage.getType();
                            if (Objects.equal(type.getKind(), ParserProtos.TypeKind.CLASS)) {
                                ClassTypeHandle clazz = type.getClazz();
                                if (clazz.getResolved()) {
                                    usageDb.write(u);
                                }
                            }
                        }
                    }
                }
            }

            // Process repository layout.
            for (File file : inputDir2.listFiles()) {
                if (file.getPath().endsWith(".crc")) {
                    continue;
                }
                SequenceFile.Reader r = new SequenceFile.Reader(fs, new Path(file.getPath()), conf);
                BytesWritable value = new BytesWritable();
                while (r.next(NullWritable.get(), value)) {
                    FileHandle f = FileHandle.parseFrom(Arrays.copyOf(value.getBytes(), value.getLength()));
                    sourceDb.writeDirectory(f.getProject(), f.getPath());
                }
            }

            LOG.info("Index files are written to " + indexDir);
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
}
