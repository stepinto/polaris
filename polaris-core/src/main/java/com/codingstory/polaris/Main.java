package com.codingstory.polaris;

import com.codingstory.polaris.indexing.JavaFileFilters;
import com.codingstory.polaris.indexing.JavaIndexer;
import com.codingstory.polaris.parser.ProjectParser;
import com.codingstory.polaris.parser.Token;
import com.codingstory.polaris.parser.TokenExtractor;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.*;
import java.text.Format;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Main {

    private static final Log LOG = LogFactory.getLog(Main.class);
    private static final Format DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                printHelp();
                System.exit(0);
            }
            String command = args[0];
            List<String> commandArgs = ImmutableList.copyOf(args).subList(1, args.length);
            if (command.equalsIgnoreCase("parse")) {
                runParse(commandArgs);
            } else if (command.equalsIgnoreCase("index")) {
                runIndex(commandArgs);
            } else if (command.equalsIgnoreCase("searchui")) {
                runSearchUI(commandArgs);
            } else {
                printHelp();
            }
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            System.exit(1);
        }
    }

    private static void runParse(List<String> commandArgs) throws IOException{
        for (String path : commandArgs) {
            File file = new File(path);
            InputStream in = null;
            try {
                System.out.println(file);
                in = new FileInputStream(file);
                List<Token> tokens = new TokenExtractor()
                        .setInputStream(in)
                        .extractTokens();
                for (Token token : tokens) {
                    System.out.println(token);
                }
            } finally {

                IOUtils.closeQuietly(in);
            }
        }
    }

    private static void runIndex(List<String> args) throws IOException {
        final JavaIndexer indexer = new JavaIndexer(new File("index"));
        int successCount = 0;
        int failureCount = 0;
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            if (args.isEmpty()) {
                printHelp();
                System.exit(1);
            }
            for (String path : args) {
                final File projectBaseDir = new File(path);
                if (!projectBaseDir.isDirectory()) {
                    LOG.error("Expect directory, but file was found: " + projectBaseDir);
                    System.exit(1);
                }
                Iterable<File> sourceFiles = FileUtils.listFiles(projectBaseDir,
                        JavaFileFilters.JAVA_SOURCE_FILETER, HiddenFileFilter.VISIBLE);

                // Pass 0: index file content
                final String projectName = projectBaseDir.getName();
                final Map<File, byte[]> fileSha1Sums = Maps.newHashMap();
                for (File sourceFile : sourceFiles) {
                    String filePath = findSourceFilePath(projectBaseDir, sourceFile);
                    InputStream in = new FileInputStream(sourceFile);
                    try {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        byte[] content = FileUtils.readFileToByteArray(sourceFile);
                        byte[] sha1sum = DigestUtils.sha(content);
                        indexer.indexFile(projectName, filePath, sha1sum, content);
                        fileSha1Sums.put(sourceFile, sha1sum);
                    } finally {
                        IOUtils.closeQuietly(in);
                    }
                }

                // Pass 1/2: index tokens
                ProjectParser parser = new ProjectParser();
                try {
                    parser.setProjectName("untitled");
                    parser.setIgnoreErrors(true);
                    for (File sourceFile : sourceFiles) {
                        parser.addSourceFile(sourceFile);
                    }
                    parser.setTokenCollector(new ProjectParser.TokenCollector() {
                        @Override
                        public void collect(File file, Token token) {
                            try {
                                String filePath = StringUtils.removeStart(
                                        file.getAbsolutePath(),
                                        projectBaseDir.getAbsolutePath());
                                indexer.indexToken(projectName, filePath, fileSha1Sums.get(file), token);
                            } catch (IOException e) {
                                throw new SkipCheckingExceptionWrapper(e);
                            }
                        }
                    });
                    parser.run();
                } catch (SkipCheckingExceptionWrapper e) {
                    throw (IOException) e.getCause();
                }
                ProjectParser.Stats stats = parser.getStats();
                successCount += stats.successFiles;
                failureCount += stats.failedFiles;
            }
        } finally {
            IOUtils.closeQuietly(indexer);
        }
        watch.stop();
        LOG.info("Completed.");
        LOG.info(String.format("Indexed source files: %d", successCount));
        LOG.info(String.format("Failed: %d", failureCount));
        LOG.info(String.format("Time elapsed: %.2fs", watch.getTime() / 1000.0));
    }

    private static String findSourceFilePath(File projectBaseDir, File sourceFile) {
        return StringUtils.removeStart(
                sourceFile.getAbsolutePath(),
                projectBaseDir.getAbsolutePath());
    }

    private static void runSearchUI(List<String> args) throws Exception {
        List<File> warFiles;
        if (args.isEmpty()) {
            warFiles = ImmutableList.copyOf(FileUtils.listFiles(new File("."),
                    new SuffixFileFilter(".war"), HiddenFileFilter.VISIBLE));
        } else {
            warFiles = Lists.transform(args, new Function<String, File>() {
                @Override
                public File apply(String s) {
                    return new File(s);
                }
            });
        }
        if (warFiles.size() != 1) {
            LOG.error(String.format("Expect exactly one WAR file, but %d was found", warFiles.size()));
            System.exit(1);
        }
        File warFile = Iterables.getOnlyElement(warFiles);
        LOG.info(String.format("Use war: %s (last modified at %s)",
                warFile.getPath(),
                DATE_FORMAT.format(new Date(warFile.lastModified()))));
        int port = 8080;
        Server server = new Server(port);
        WebAppContext ctx = new WebAppContext();
        ctx.setWar(warFile.getPath());
        server.setHandler(ctx);
        server.start();
        LOG.info(String.format("Listening at http://localhost:%d", port));
        server.join();
    }

    private static void printHelp() {
        System.out.println("Usage:");
        System.out.println("  polaris parse dir/files...");
        System.out.println("  polaris index dir/files...");
        System.out.println("  polaris searchui");
        System.out.println();
    }
}
