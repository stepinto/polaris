package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import com.codingstory.polaris.search.CodeSearchServiceImpl;
import com.codingstory.polaris.search.TCodeSearchService;
import com.codingstory.polaris.search.THit;
import com.codingstory.polaris.search.TSearchRequest;
import com.codingstory.polaris.search.TSearchResponse;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.io.File;
import java.io.IOException;

import static com.codingstory.polaris.cli.CommandUtils.die;
import static com.codingstory.polaris.cli.CommandUtils.openSocket;

@Command(name = "search")
public class Search {
    @Option(name = "index", shortName = "i", defaultValue = "index")
    public String index;

    @Option(name = "server", shortName = "s", defaultValue = "")
    public String server;

    @Run
    public void run(String[] args) throws IOException, TException {
        if (args.length != 1) {
            die("Require exactly one query");
        }
        String query = args[0];
        if (!(Strings.isNullOrEmpty(index) ^ Strings.isNullOrEmpty(server))) {
            die("Require either --index=<dir> or --server=<ip:port> is specified");
        }
        TTransport transport = null;
        try {
            TCodeSearchService.Iface client;
            if (!Strings.isNullOrEmpty(index)) {
                client = new CodeSearchServiceImpl(new File(index));
            } else {
                TSocket socket = openSocket(server);
                socket.open();
                transport = new TFramedTransport(socket);
                client = new TCodeSearchService.Client(new TBinaryProtocol(transport));
            }

            TSearchRequest req = new TSearchRequest()
                    .setQuery(query)
                    .setRankFrom(0)
                    .setRankTo(20);
            TSearchResponse resp = client.search(req);
            int i = 0;
            for (THit hit : resp.getHits()) {
                System.out.printf("%d: %s/%s (%.2f)\n",
                        i++,
                        hit.getProject(),
                        hit.getPath(),
                        hit.getScore());
                for (String line : Splitter.on("\n").split(hit.getSummary())) {
                    System.out.println("  " + line);
                }
            }
        } finally {
            if (transport != null) {
                transport.close();
            }
        }
    }

    @Help
    public void help() {
        System.out.println("Usage: \n" +
                "  polaris search <query> [--index=<dir>] [--server=<ip:port>]\n" +
                "\n" +
                "Options:\n" +
                "  --index=<dir>        the index directory, default: ./index\n" +
                "  --server=<ip:port>   the search server to request from, default: null\n" +
                "\n");
    }
}
