package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import com.codingstory.polaris.search.CodeSearchServiceImpl;
import com.codingstory.polaris.search.TCodeSearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.File;
import java.io.IOException;

import static com.codingstory.polaris.cli.CommandUtils.die;

@Command(name = "searchserver")
public class SearchServer {

    private static final Log LOG = LogFactory.getLog(SearchServer.class);

    @Option(name = "index", shortName = "i", defaultValue = "index")
    public String index;

    @Option(name = "port", shortName = "p", defaultValue = "5000")
    public String port;

    @Run
    public void run(String[] args) throws TTransportException, IOException {
        if (args.length > 0) {
            die("Found more arguments: " + args);
        }

        File indexDir = new File(index);
        TServerTransport transport = new TServerSocket(Integer.parseInt(port));
        TCodeSearchService.Processor<TCodeSearchService.Iface> processor =
                new TCodeSearchService.Processor<TCodeSearchService.Iface>(
                        new CodeSearchServiceImpl(indexDir));
        TBinaryProtocol.Factory protocolFactory = new TBinaryProtocol.Factory();
        TThreadPoolServer server = new TThreadPoolServer(
                new TThreadPoolServer.Args(transport)
                        .minWorkerThreads(1)
                        .maxWorkerThreads(10)
                        .processor(processor)
                        .transportFactory(new TFramedTransport.Factory())
                        .protocolFactory(protocolFactory));
        LOG.info("Index dir: " + indexDir);
        LOG.info("Starting searcher at port " + port);
        server.serve();
    }

    @Help
    public void help() {
        System.out.println("Usage: \n" +
                "  polaris searchserver [--index=<dir>] [--port=<port>]\n" +
                "\n" +
                "Options:\n" +
                "  -i, --index=<dir>        the index directory, default: ./index\n" +
                "  -p, --port=<port>        port to listen on, default: 5000\n" +
                "\n");
    }
}
