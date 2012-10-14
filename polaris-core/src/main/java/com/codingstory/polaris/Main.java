package com.codingstory.polaris;

import com.codingstory.polaris.indexing.IndexBuilder;
import com.codingstory.polaris.parser.*;
import com.codingstory.polaris.search.CodeSearchServiceImpl;
import com.codingstory.polaris.search.TCodeSearchService;
import com.codingstory.polaris.search.TSearchRequest;
import com.codingstory.polaris.search.TSearchResponse;
import com.codingstory.polaris.search.TSearchResultEntry;
import com.codingstory.polaris.search.TStatusCode;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
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
    private static final int SERVER_DEFAULT_PORT = 5000;

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
            } else if (command.equalsIgnoreCase("search")) {
                runSearch(commandArgs);
            } else if (command.equalsIgnoreCase("searchserver")) {
                runSearchServer(commandArgs);
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
                List<Token> tokens = TokenExtractor.extract(in, TypeResolver.NO_OP_RESOLVER);
                for (Token token : tokens) {
                    System.out.println(token);
                }
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }

    private static void runIndex(List<String> args) throws IOException {
        if (args.isEmpty()) {
            printHelp();
            System.exit(1);
        }
        List<File> projectDirs = Lists.newArrayList();
        for (String path : args) {
            projectDirs.add(new File(path));
        }
        ParserOptions parserOptions = new ParserOptions();
        parserOptions.setFailFast(false);
        IndexBuilder indexBuilder = new IndexBuilder();
        indexBuilder.setIndexDirectory(new File("index"));
        indexBuilder.setParserOptions(parserOptions);
        indexBuilder.setProjectDirectories(projectDirs);
        indexBuilder.build();
    }

    private static String findSourceFilePath(File projectBaseDir, File sourceFile) {
        return StringUtils.removeStart(
                sourceFile.getAbsolutePath(),
                projectBaseDir.getAbsolutePath());
    }

    private static void runSearchServer(List<String> args) throws Exception {
        Options commandLineOptions = new Options();
        commandLineOptions.addOption(new Option("p", "port", true, "the port to listen for RPC"));
        commandLineOptions.addOption(new Option("i", "index-dir", true, "index dir"));
        CommandLineParser commandLineParser = new GnuParser();
        CommandLine commandLine = commandLineParser.parse(commandLineOptions, stringListToArray(args));
        String portStr = commandLine.getOptionValue("port", String.valueOf(SERVER_DEFAULT_PORT));
        int port = Integer.parseInt(portStr);
        File indexDir = new File(commandLine.getOptionValue("index-dir", "index"));

        TServerTransport transport = new TServerSocket(port);
        TCodeSearchService.Processor processor = new TCodeSearchService.Processor(
                new CodeSearchServiceImpl(indexDir));
        TBinaryProtocol.Factory protocolFactory = new TBinaryProtocol.Factory();
        TSimpleServer server = new TSimpleServer(
                new TSimpleServer.Args(transport)
                        .processor(processor)
                        .protocolFactory(protocolFactory));
        LOG.info("Starting searcher at port " + port);
        server.serve();
    }

    private static String[] stringListToArray(List<String> a) {
        return a.toArray(new String[a.size()]);
    }

    private static void runSearch(List<String> args) throws Exception {
        Options commandLineOptions = new Options();
        commandLineOptions.addOption(new Option("h", "host", true, "Host to connect"));
        commandLineOptions.addOption(new Option("p", "port", true, "Port to connect"));
        CommandLineParser commandLineParser = new GnuParser();
        CommandLine commandLine = commandLineParser.parse(commandLineOptions, stringListToArray(args));
        String host = commandLine.getOptionValue("host", "127.0.0.1");
        int port = Integer.parseInt(commandLine.getOptionValue("port", String.valueOf(SERVER_DEFAULT_PORT)));
        TTransport transport = new TSocket(host, port);
        TProtocol protocol = new TBinaryProtocol(transport);
        transport.open();
        TCodeSearchService.Client client = new TCodeSearchService.Client(protocol);

        for (String query : commandLine.getArgs()) {
            System.out.println("Query: " + query);
            TSearchRequest req = new TSearchRequest();
            req.setQuery(query);
            TSearchResponse resp = client.search(req);
            if (resp.getStatus() != TStatusCode.OK) {
                die("Status: " + resp.getStatus());
            }
            int i = 1;
            System.out.printf("Latency: %.2f ms\n", resp.getLatency() / 1000.0);
            System.out.println("Results: " + resp.getCount());
            for (TSearchResultEntry e : resp.getEntries()) {
                System.out.println("Result #" + i + ": " + e.getKind());
                System.out.println(e.getSummary());
                i++;
            }
        }

        transport.close();
    }

    private static void die(String s) {
        LOG.fatal(s);
        System.exit(1);
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
        System.out.println("  polaris searchserver");
        System.out.println("  polaris search query");
        System.out.println();
    }
}
