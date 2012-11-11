package com.codingstory.polaris.indexing;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.SimpleIdGenerator;
import com.codingstory.polaris.SkipCheckingExceptionWrapper;
import com.codingstory.polaris.parser.ClassType;
import com.codingstory.polaris.parser.ParserOptions;
import com.codingstory.polaris.parser.ProjectParser;
import com.codingstory.polaris.parser.SourceFile;
import com.codingstory.polaris.parser.TypeHandle;
import com.codingstory.polaris.parser.TypeUsage;
import com.codingstory.polaris.parser.TypeUtils;
import com.codingstory.polaris.parser.Usage;
import com.codingstory.polaris.sourcedb.SourceDbWriter;
import com.codingstory.polaris.sourcedb.SourceDbWriterImpl;
import com.codingstory.polaris.typedb.TypeDb;
import com.codingstory.polaris.typedb.TypeDbImpl;
import com.codingstory.polaris.typedb.TypeDbWriter;
import com.codingstory.polaris.typedb.TypeDbWriterImpl;
import com.codingstory.polaris.usagedb.UsageDbWriter;
import com.codingstory.polaris.usagedb.UsageDbWriterImpl;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class IndexBuilder {
    private static final Log LOG = LogFactory.getLog(IndexBuilder.class);

    public static class Stats {
        public int successes;
        public int failures;
        public int types;
    }

    private static class PushToDbCollector implements ProjectParser.TypeCollector,
            ProjectParser.AnnotatedSourceCollector, ProjectParser.UsageCollector {
        private final TypeDbWriter typeDbWriter;
        private final SourceDbWriter sourceDbWriter;
        private final UsageDbWriter usageDbWriter;

        private PushToDbCollector(
                TypeDbWriter typeDbWriter,
                SourceDbWriter sourceDbWriter,
                UsageDbWriter usageDbWriter) {
            this.typeDbWriter = Preconditions.checkNotNull(typeDbWriter);
            this.sourceDbWriter = Preconditions.checkNotNull(sourceDbWriter);
            this.usageDbWriter = Preconditions.checkNotNull(usageDbWriter);
        }

        @Override
        public void collectType(File file, List<ClassType> types) {
            Preconditions.checkNotNull(types);
            try {
                for (ClassType type : types) {
                    typeDbWriter.write(type);
                }
            } catch (IOException e) {
                throw new SkipCheckingExceptionWrapper(e);
            }
        }

        @Override
        public void collectSource(SourceFile source) {
            Preconditions.checkNotNull(source);
            try {
                sourceDbWriter.writeSourceFile(source);
            } catch (IOException e) {
                throw new SkipCheckingExceptionWrapper(e);
            }
        }

        @Override
        public void collectUsage(File file, List<Usage> usages) {
            Preconditions.checkNotNull(usages);
            try {
                for (Usage usage : usages) {
                    if (usage instanceof TypeUsage) {
                        TypeUsage typeUsage = (TypeUsage) usage;
                        TypeHandle type = typeUsage.getType();
                        if (type.isResolved() && !TypeUtils.isPrimitiveTypeHandle(type)) {
                            usageDbWriter.write(typeUsage);
                        }
                    }
                }
            } catch (IOException e) {
                throw new SkipCheckingExceptionWrapper(e);
            }
        }
    }

    private File indexDirectory;
    private List<File> projectDirectories;
    private final Stats stats = new Stats();
    private ParserOptions parserOptions = new ParserOptions();
    private IdGenerator idGenerator = new SimpleIdGenerator(); // TODO: checkpoint it

    public IndexBuilder() {}

    public void setIndexDirectory(File indexDirectory) {
        this.indexDirectory = indexDirectory;
    }

    public void setProjectDirectories(List<File> projectDirectories) {
        this.projectDirectories = projectDirectories;
    }

    public void setParserOptions(ParserOptions parserOptions) {
        this.parserOptions = parserOptions;
    }

    public void build() throws IOException {
        Preconditions.checkNotNull(indexDirectory);
        Preconditions.checkNotNull(projectDirectories);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        prepareIndexDirectory();
        for (File projectDir : projectDirectories) {
            if (!projectDir.isDirectory()) {
                throw new IOException("Expect project dir: " + projectDir);
            }
            buildIndexForProject(projectDir);
        }
        stopWatch.stop();
        LOG.info("Completed.");
        LOG.info(String.format("Indexed types: %d", stats.types));
        LOG.info(String.format("Indexed source files: %d", stats.successes));
        LOG.info(String.format("Failed: %d", stats.failures));
        LOG.info(String.format("Time elapsed: %.2fs", stopWatch.getTime() / 1000.0));
    }

    private void buildIndexForProject(final File projectDir) throws IOException {
        Preconditions.checkNotNull(projectDir);
        Preconditions.checkNotNull(projectDir.isDirectory());
        String projectName = projectDir.getName();
        LOG.info("Indexing project: " + projectName);
        List<File> sourceFiles = ImmutableList.copyOf(FileUtils.listFiles(projectDir,
                JavaFileFilters.JAVA_SOURCE_FILETER, HiddenFileFilter.VISIBLE));
        LOG.info("Found " + sourceFiles.size() + " source file(s)");
        File typeDbPath = IndexPathUtils.getTypeDbPath(indexDirectory);
        TypeDb typeDb = null;
        TypeDbWriter typeDbWriter = null;
        TypeDb sourceDb = null;
        SourceDbWriter sourceDbWriter = null;
        TypeDb usageDb = null;
        UsageDbWriter usageDbWriter = null;
        try {
            typeDb = new TypeDbImpl(typeDbPath);
            typeDbWriter = new TypeDbWriterImpl(typeDbPath);
            File sourceDbPath = IndexPathUtils.getSourceDbPath(indexDirectory);
            sourceDb = new TypeDbImpl(sourceDbPath);
            sourceDbWriter = new SourceDbWriterImpl(sourceDbPath);
            File usageDbPath = IndexPathUtils.getUsageDbPath(indexDirectory);
            usageDb = new TypeDbImpl(usageDbPath);
            usageDbWriter = new UsageDbWriterImpl(usageDbPath);

            // Index source files
            PushToDbCollector collector = new PushToDbCollector(typeDbWriter, sourceDbWriter, usageDbWriter);
            ProjectParser parser = new ProjectParser();
            parser.setExternalTypeResolver(new CrossTypeResolver(typeDb));
            parser.setParserOptions(parserOptions);
            for (File sourceFile : sourceFiles) {
                parser.addSourceFile(sourceFile);
            }
            parser.setTypeCollector(collector);
            parser.setAnnotatedSourceCollector(collector);
            parser.setUsageCollector(collector);
            parser.setProjectName(projectName);
            parser.setIdGenerator(idGenerator);
            parser.setProjectBaseDirectory(projectDir);
            parser.run();

            ProjectParser.Stats parserStats = parser.getStats();
            stats.successes += parserStats.successFiles;
            stats.failures += parserStats.failedFiles;
            if (stats.failures > 0) {
                LOG.warn("Failed to index " + stats.failures + " source file(s)");
            }

            // Index project layout
            final List<File> sourceDirs = Lists.newArrayList();
            DirectoryTranverser.traverse(projectDir, new DirectoryTranverser.Visitor() {
                @Override
                public void visit(File file) {
                    if (file.isDirectory() && !file.isHidden()) {
                        sourceDirs.add(file);
                    }
                }
            });
            for (File dir : sourceDirs) {
                sourceDbWriter.writeDirectory(projectName, findSourceFilePath(projectDir, dir));
            }
        } catch (SkipCheckingExceptionWrapper e) {
            throw (IOException) e.getCause();
        } finally {
            IOUtils.closeQuietly(typeDb);
            IOUtils.closeQuietly(typeDbWriter);
            IOUtils.closeQuietly(sourceDb);
            IOUtils.closeQuietly(sourceDbWriter);
            IOUtils.closeQuietly(usageDb);
            IOUtils.closeQuietly(usageDbWriter);
        }
    }

    private static String findSourceFilePath(File projectDir, File sourceFile) {
        return StringUtils.removeStart(
                sourceFile.getAbsolutePath(),
                projectDir.getAbsolutePath());
    }

    private void prepareIndexDirectory() throws IOException {
        if (indexDirectory.exists()) {
            LOG.info("Cleaning index directory: " + indexDirectory);
            FileUtils.cleanDirectory(indexDirectory);
        } else {
            LOG.info("Create index directory: " + indexDirectory);
            if (!indexDirectory.mkdirs()) {
                throw new IOException("Failed to mkdir: " + indexDirectory);
            }
        }

        TypeDbWriter typeDbCreator = new TypeDbWriterImpl(IndexPathUtils.getTypeDbPath(indexDirectory));
        typeDbCreator.flush();
        typeDbCreator.close();
        SourceDbWriter sourceDbCreator = new SourceDbWriterImpl(IndexPathUtils.getSourceDbPath(indexDirectory));
        sourceDbCreator.flush();
        sourceDbCreator.close();
        UsageDbWriter usageDbCreator = new UsageDbWriterImpl(IndexPathUtils.getUsageDbPath(indexDirectory));
        usageDbCreator.flush();
        usageDbCreator.close();
    }
}
