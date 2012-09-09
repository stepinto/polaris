package com.codingstory.polaris.indexing;

import com.codingstory.polaris.SkipCheckingExceptionWrapper;
import com.codingstory.polaris.parser.ParserOptions;
import com.codingstory.polaris.parser.ProjectParser;
import com.codingstory.polaris.parser.Token;
import com.google.common.base.Preconditions;
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

        final JavaIndexer indexer = new JavaIndexer(indexDirectory);
        try {
            for (File projectDir : projectDirectories) {
                buildIndexForProject(projectDir, indexer);
            }
        } finally {
            IOUtils.closeQuietly(indexer);
        }

        stopWatch.stop();
        LOG.info("Completed.");
        LOG.info(String.format("Indexed source files: %d", stats.successes));
        LOG.info(String.format("Failed: %d", stats.failures));
        LOG.info(String.format("Time elapsed: %.2fs", stopWatch.getTime() / 1000.0));
    }

    private void buildIndexForProject(final File projectDir, final JavaIndexer indexer) throws IOException {
        Preconditions.checkNotNull(projectDir);
        Preconditions.checkNotNull(indexer);
        if (!projectDir.isDirectory()) {
            LOG.error("Expect directory, but file was found: " + projectDir);
            System.exit(1);
        }
        Iterable<File> sourceFiles = FileUtils.listFiles(projectDir,
                JavaFileFilters.JAVA_SOURCE_FILETER, HiddenFileFilter.VISIBLE);

        final String projectName = projectDir.getName();
        ProjectParser parser = new ProjectParser();
        try {
            parser.setParserOptions(parserOptions);
            for (File sourceFile : sourceFiles) {
                parser.addSourceFile(sourceFile);
            }
            parser.setTokenCollector(new ProjectParser.TokenCollector() {
                @Override
                public void collect(File file, byte[] content, List<Token> tokens) {
                    try {
                        String filePath = findSourceFilePath(projectDir, file);
                        indexer.indexFile(projectName, filePath, content, tokens);
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
    }

    private static String findSourceFilePath(File projectDir, File sourceFile) {
        return StringUtils.removeStart(
                sourceFile.getAbsolutePath(),
                projectDir.getAbsolutePath());
    }
}
