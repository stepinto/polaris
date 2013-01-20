package com.codingstory.polaris.cli;

import com.codingstory.polaris.indexing.IndexBuilder;
import com.codingstory.polaris.parser.ParserOptions;
import com.codingstory.polaris.search.CodeSearchServiceImpl;
import com.codingstory.polaris.search.TCodeSearchService;
import com.codingstory.polaris.search.TStatusCode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.util.IOUtils;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandUtils {
    private CommandUtils() {}

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("([A-Za-z0-9\\.]+):([0-9]+)");
    private static final Log LOG = LogFactory.getLog(CommandUtils.class);

    public static interface RpcRunner {
        void run(TCodeSearchService.Iface rpc) throws TException;
    }

    public static void die(String message) {
        System.err.println("polaris: " + message);
        System.exit(1);
    }

    public static TSocket openSocket(String address) {
        Matcher m = ADDRESS_PATTERN.matcher(address);
        if (!m.matches()) {
            throw new IllegalArgumentException("Address: " + address);
        }
        String host = m.group(1);
        int port = Integer.parseInt(m.group(2));
        LOG.info("Connect to " + host + ":" + port);
        return new TSocket(host, port);
    }

    public static IndexBuilder createIndexer(File output) throws IOException {
        ParserOptions parserOptions = new ParserOptions();
        parserOptions.setFailFast(false);
        IndexBuilder indexBuilder = new IndexBuilder();
        indexBuilder.setIndexDirectory(output);
        indexBuilder.setParserOptions(parserOptions);
        return indexBuilder;
    }

    public static void openIndexOrConnectToServerAndRun(String index, String server, RpcRunner runner)
            throws TException, IOException {
        if (!Strings.isNullOrEmpty(server)) {
            connectToServerAndRun(server, runner);
        } else {
            openIndexAndRun(new File(index), runner);
        }
    }

    public static void connectToServerAndRun(String spec, RpcRunner runner) throws TException {
        Preconditions.checkArgument(Strings.isNullOrEmpty(spec));
        Preconditions.checkNotNull(runner);
        TSocket socket = openSocket(spec);
        TTransport transport = null;
        try {
            socket.open();
            transport = new TFramedTransport(socket);
            runner.run(new TCodeSearchService.Client(new TBinaryProtocol(transport)));
        } finally {
            if (transport != null) {
                transport.close();
            }
        }
    }

    public static void openIndexAndRun(File index, RpcRunner runner) throws IOException, TException {
        Preconditions.checkNotNull(index);
        Preconditions.checkNotNull(runner);
        CodeSearchServiceImpl rpc = null;
        try {
            rpc = new CodeSearchServiceImpl(index);
            runner.run(rpc);
        } finally {
            IOUtils.close(rpc);
        }
    }

    public static void checkStatus(TStatusCode statusCode) {
        if (!Objects.equal(TStatusCode.OK, statusCode)) {
            die("Server error: " + statusCode.name() + "(" + statusCode.ordinal() + ")");
        }
    }
}
