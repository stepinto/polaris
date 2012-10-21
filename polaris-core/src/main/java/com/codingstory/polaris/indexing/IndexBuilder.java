package com.codingstory.polaris.indexing;

import com.codingstory.polaris.SkipCheckingExceptionWrapper;
import com.codingstory.polaris.indexing.analysis.JavaSrcAnalyzer;
import com.codingstory.polaris.indexing.layout.LayoutIndexer;
import com.codingstory.polaris.parser.ParserOptions;
import com.codingstory.polaris.parser.ProjectParser;
import com.codingstory.polaris.parser.Token;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public final class IndexBuilder {
    private static final Log LOG = LogFactory.getLog(IndexBuilder.class);

    public static class Stats {
        public int successes;
        public int failures;
        public int tokens;
    }

    private File indexDirectory;
    private List<File> projectDirectories;
    private final Stats stats = new Stats();
    private ParserOptions parserOptions = new ParserOptions();

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

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, new JavaSrcAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(FSDirectory.open(indexDirectory), config);
        try {
            for (File projectDir : projectDirectories) {
                buildIndexForProject(projectDir, writer);
            }
        } finally {
            writer.close();
        }

        stopWatch.stop();
        LOG.info("Completed.");
        LOG.info(String.format("Indexed tokens: %d", stats.tokens));
        LOG.info(String.format("Indexed source files: %d", stats.successes));
        LOG.info(String.format("Failed: %d", stats.failures));
        LOG.info(String.format("Time elapsed: %.2fs", stopWatch.getTime() / 1000.0));
    }

    private void buildIndexForProject(final File projectDir, IndexWriter writer) throws IOException {
        Preconditions.checkNotNull(projectDir);
        Preconditions.checkNotNull(writer);
        if (!projectDir.isDirectory()) {
            LOG.error("Expect directory, but file was found: " + projectDir);
            System.exit(1);
        }
        Iterable<File> sourceFiles = FileUtils.listFiles(projectDir,
                JavaFileFilters.JAVA_SOURCE_FILETER, HiddenFileFilter.VISIBLE);
        String projectName = projectDir.getName();

        // Index source files
        ProjectParser parser = new ProjectParser();
        final JavaIndexer javaIndexer = new JavaIndexer(writer, projectName);
        Set<File> sourceDirs = Sets.newHashSet();
        try {
            parser.setParserOptions(parserOptions);
            for (File sourceFile : sourceFiles) {
                parser.addSourceFile(sourceFile);
                sourceDirs.add(sourceFile.getParentFile());
            }
            parser.setTokenCollector(new ProjectParser.TokenCollector() {
                @Override
                public void collect(File file, byte[] content, List<Token> tokens) {
                    try {
                        String filePath = findSourceFilePath(projectDir, file);
                        javaIndexer.indexFile(filePath, content, tokens);
                        stats.tokens += tokens.size();
                    } catch (IOException e) {
                        throw new SkipCheckingExceptionWrapper(e);
                    }
                }
            });
            parser.run();
        } catch (SkipCheckingExceptionWrapper e) {
            throw (IOException) e.getCause();
        }
        ProjectParser.Stats parserStats = parser.getStats();
        stats.successes += parserStats.successFiles;
        stats.failures += parserStats.failedFiles;

        // Index project layout
        LayoutIndexer layoutIndexer = new LayoutIndexer(writer, projectName, projectDir);
        for (File dir : sourceDirs) {
            layoutIndexer.indexDirectory(dir);
        }
    }

    private static String findSourceFilePath(File projectDir, File sourceFile) {
        return StringUtils.removeStart(
                sourceFile.getAbsolutePath(),
                projectDir.getAbsolutePath());
    }
}
