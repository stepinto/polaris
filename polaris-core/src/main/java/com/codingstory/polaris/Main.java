package com.codingstory.polaris;

import com.codingstory.polaris.indexing.IndexBuilder;
import com.codingstory.polaris.parser.ParserOptions;
import com.codingstory.polaris.parser.SecondPassProcessor;
import com.codingstory.polaris.parser.TypeResolver;
import com.codingstory.polaris.parser.Usage;
import com.codingstory.polaris.repo.GitHubUtils;
import com.codingstory.polaris.repo.GitUtils;
import com.codingstory.polaris.repo.Repository;
import com.codingstory.polaris.search.CodeSearchServiceImpl;
import com.codingstory.polaris.search.TCodeSearchService;
import com.codingstory.polaris.search.THit;
import com.codingstory.polaris.search.TSearchRequest;
import com.codingstory.polaris.search.TSearchResponse;
import com.codingstory.polaris.search.TSourceRequest;
import com.codingstory.polaris.search.TSourceResponse;
import com.codingstory.polaris.search.TStatusCode;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TThreadPoolServer;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                runIndexDir(commandArgs);
            } else if (command.equalsIgnoreCase("indexrepobase")) {
                runIndexRepoBase(commandArgs);
            } else if (RPC_COMMAND_TABLE.containsKey(command.toLowerCase())) {
                runRpc(RPC_COMMAND_TABLE.get(command), commandArgs);
            } else if (command.equalsIgnoreCase("searchserver")) {
                runSearchServer(commandArgs);
            } else if (command.equalsIgnoreCase("crawlgithub")) {
                runCrawlGitHub(commandArgs);
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
                long fakeFileId = 1;
                SecondPassProcessor.Result result = SecondPassProcessor.extract(
                        "temp-project",
                        fakeFileId,
                        in,
                        TypeResolver.NO_OP_RESOLVER,
                        new SimpleIdGenerator());
                for (Usage usage : result.getUsages()) {
                    System.out.println(usage);
                }
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }

    private static void runIndexDir(List<String> args) throws IOException {
        if (args.isEmpty()) {
            printHelp();
            System.exit(1);
        }
        IndexBuilder indexer = createIndexer(new File("index"));
        for (String path : args) {
            File dir = new File(path);
            indexer.indexDirectory(dir);
        }
    }

    private static void runIndexRepoBase(List<String> args) throws IOException {
        if (args.isEmpty()) {
            printHelp();
            System.exit(1);
        }
        IndexBuilder indexer = createIndexer(new File("index"));
        for (String arg : args) {
            LOG.info("Found repobase: " + arg);
            for (Repository repo : GitUtils.openRepoBase(new File(arg))) {
                indexer.indexRepository(repo);
            }
        }
    }

    private static IndexBuilder createIndexer(File output) throws IOException {
        FileUtils.deleteDirectory(output);
        ParserOptions parserOptions = new ParserOptions();
        parserOptions.setFailFast(false);
        IndexBuilder indexBuilder = new IndexBuilder();
        indexBuilder.setIndexDirectory(output);
        indexBuilder.setParserOptions(parserOptions);
        return indexBuilder;
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
        TCodeSearchService.Processor<TCodeSearchService.Iface> processor =
                new TCodeSearchService.Processor<TCodeSearchService.Iface>(
                        new CodeSearchServiceImpl(indexDir));
        TBinaryProtocol.Factory protocolFactory = new TBinaryProtocol.Factory();
        TThreadPoolServer server = new TThreadPoolServer(
                new TThreadPoolServer.Args(transport)
                        .minWorkerThreads(1)
                        .maxWorkerThreads(10)
                        .processor(processor)
                        .protocolFactory(protocolFactory));
        LOG.info("Starting searcher at port " + port);
        server.serve();
    }

    private static void runCrawlGitHub(List<String> commandArgs) throws Exception {
        Options commandLineOptions = new Options();
        commandLineOptions.addOption(new Option("o", "output", true, "output directory"));
        CommandLineParser commandLineParser = new GnuParser();
        CommandLine commandLine = commandLineParser.parse(commandLineOptions, stringListToArray(commandArgs));
        String outputPath = commandLine.getOptionValue("output");
        File outputDir = new File(outputPath);
        if (outputDir.isDirectory()) {
            LOG.info("Reuse existing repo base directory: " + outputPath);
        } else {
            outputDir.mkdir();
        }
        Pattern userPattern = Pattern.compile("([A-Za-z0-9]+)");
        Pattern userRepoPattern = Pattern.compile("([A-Za-z0-9]+)/([A-Za-z0-9]+)");
        List<Repository> repos = Lists.newArrayList();
        for (String arg : commandLine.getArgs()) {
            Matcher m;
            if ((m = userPattern.matcher(arg)).matches()) {
                String user = m.group(1);
                repos.addAll(GitHubUtils.listUserRepositories(user));
            } else if ((m = userRepoPattern.matcher(arg)).matches()) {
                String user = m.group(1);
                String repo = m.group(2);
                repos.add(GitHubUtils.getRepository(user, repo));
            } else {
                LOG.error("bad repo: " + arg);
                System.exit(1);
            }
        }
        LOG.info("Need to clone " + repos.size() + " repo(s)");
        for (Repository repo : repos) {
            int retry = 10;
            while (retry > 0) {
                try {
                    GitUtils.mirrorOrSync(repo, outputDir);
                    break;
                } catch (IOException e) {
                    if (retry >= 0) {
                        LOG.warn("Retry on exception", e);
                        Thread.sleep(10);
                        retry--;
                    } else {
                        throw e;
                    }
                }
            }
        }
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
                    req.setRankFrom(0);
                    req.setRankTo(20);
                    TSearchResponse resp = client.search(req);
                    checkRpcStatus(resp.getStatus());
                    int i = 1;
                    System.out.printf("Latency: %.2f ms\n", resp.getLatency() / 1000.0);
                    System.out.println("Results: " + resp.getCount());
                    for (THit hit : resp.getHits()) {
                        System.out.println("Result #" + i + ": ");
                        System.out.println(hit.getSummary());
                        i++;
                    }
                    if (resp.getHitsSize() == 20) {
                        System.out.println("Only top 20 hits are displayed.");
                    }
                }
            } else if (command == RpcCommand.SOURCE) {
                for (String path : commandLine.getArgs()) {
                    TSourceRequest req = new TSourceRequest();
                    if (path.matches("\\d+")) {
                        req.setFileId(Long.parseLong(path));
                    } else {
                        int slashPos = path.indexOf('/');
                        String project = path.substring(0, slashPos);
                        String fileName = path.substring(slashPos + 1);
                        req.setProjectName(project);
                        req.setFileName(fileName);
                    }
                    TSourceResponse resp = client.source(req);
                    checkRpcStatus(resp.getStatus());
                    System.out.println(">> " + path);
                    System.out.println(resp.getSource().getAnnotatedSource());
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
        System.out.println("  polaris indexrepobase repobase...");
        System.out.println("  polaris searchserver");
        System.out.println("  polaris search query");
        System.out.println("  polaris source source");
        System.out.println("  polaris crawlgithub user -o repobase");
        System.out.println("  polaris crawlgithub user/repo -o repobase");
        System.out.println();
    }
}
