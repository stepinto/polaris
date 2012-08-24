package com.codingstory.polaris;

import com.codingstory.polaris.indexing.JavaFileFilters;
import com.codingstory.polaris.indexing.JavaIndexer;
import com.codingstory.polaris.parser.JavaTokenExtractor;
import com.codingstory.polaris.parser.Token;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Format;
import java.util.Date;
import java.util.List;

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
                List<Token> tokens = new JavaTokenExtractor()
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
        JavaIndexer indexer = new JavaIndexer(new File("index"));
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
                File file = new File(path);
                Iterable<File> sourceFiles;
                if (file.isDirectory()) {
                    sourceFiles = FileUtils.listFiles(file,
                            JavaFileFilters.JAVA_SOURCE_FILETER, HiddenFileFilter.VISIBLE);
                } else if (file.isFile()) {
                    sourceFiles = ImmutableList.of(file);
                } else {
                    LOG.warn(String.format("Expect file or directory, but %s was found. Ignore it!", file));
                    sourceFiles = ImmutableList.of();
                }
                for (File sourceFile : sourceFiles) {
                    try {
                        indexer.indexFile(sourceFile);
                        successCount++;
                    } catch (IOException e) {
                        LOG.error(String.format("Caught exception when indexing", file), e);
                        failureCount++;
                    }
                }
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
