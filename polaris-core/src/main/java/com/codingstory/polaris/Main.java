package com.codingstory.polaris;

import com.codingstory.polaris.indexing.FileId;
import com.codingstory.polaris.indexing.IndexBuilder;
import com.codingstory.polaris.parser.ParserOptions;
import com.codingstory.polaris.parser.Token;
import com.codingstory.polaris.parser.TokenExtractor;
import com.codingstory.polaris.parser.TypeResolver;
import com.codingstory.polaris.search.*;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Format;
import java.util.List;
import java.util.Map;

public class Main {

    private static final Log LOG = LogFactory.getLog(Main.class);
    private static final Format DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
    private static final int SERVER_DEFAULT_PORT = 5000;

    private static enum RpcCommand {
        SEARCH,
        SOURCE
    }
    private static Map<String, RpcCommand> RPC_COMMAND_TABLE = Maps.uniqueIndex(
            ImmutableList.copyOf(RpcCommand.values()),
            new Function<RpcCommand, String>() {
                @Override
                public String apply(RpcCommand command) {
                    return command.name().toLowerCase();
                }
            });

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
            } else if (RPC_COMMAND_TABLE.containsKey(command.toLowerCase())) {
                runRpc(RPC_COMMAND_TABLE.get(command), commandArgs);
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

    private static void runRpc(RpcCommand command, List<String> args) throws Exception {
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

        try {
            TCodeSearchService.Client client = new TCodeSearchService.Client(protocol);
            if (command == RpcCommand.SEARCH) {
                for (String query : commandLine.getArgs()) {
                    System.out.println(">> " + query);
                    TSearchRequest req = new TSearchRequest();
                    req.setQuery(query);
                    TSearchResponse resp = client.search(req);
                    checkRpcStatus(resp.getStatus());
                    int i = 1;
                    System.out.printf("Latency: %.2f ms\n", resp.getLatency() / 1000.0);
                    System.out.println("Results: " + resp.getCount());
                    for (TSearchResultEntry e : resp.getEntries()) {
                        System.out.println("Result #" + i + ": " + e.getKind());
                        System.out.println(e.getSummary());
                        i++;
                    }
                }
            } else if (command == RpcCommand.SOURCE) {
                for (String fileId : commandLine.getArgs()) {
                    TSourceRequest req = new TSourceRequest();
                    req.setFileId(new FileId(fileId).getValue());
                    TSourceResponse resp = client.source(req);
                    checkRpcStatus(resp.getStatus());
                    System.out.println(">> " + fileId);
                    System.out.println(resp.getAnnotations());
                }
            } else {
                LOG.fatal("Unsupported command: " + command);
            }
        } finally {
            transport.close();
        }
    }

    private static void checkRpcStatus(TStatusCode status) {
        if (status != TStatusCode.OK) {
            die("Status: " + status);
        }
    }

    private static void die(String s) {
        LOG.fatal(s);
        System.exit(1);
    }

    private static void printHelp() {
        System.out.println("Usage:");
        System.out.println("  polaris parse dir/files...");
        System.out.println("  polaris index dir/files...");
        System.out.println("  polaris searchserver");
        System.out.println("  polaris search query");
        System.out.println("  polaris source file-id");
        System.out.println();
    }
}
