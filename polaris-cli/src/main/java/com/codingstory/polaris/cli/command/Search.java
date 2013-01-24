package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.CommandUtils;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import com.codingstory.polaris.search.TCodeSearchService;
import com.codingstory.polaris.search.THit;
import com.codingstory.polaris.search.TSearchRequest;
import com.codingstory.polaris.search.TSearchResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import org.apache.thrift.TException;

import java.io.IOException;

import static com.codingstory.polaris.cli.CommandUtils.die;

@Command(name = "search")
public class Search {
    @Option(name = "index", shortName = "i", defaultValue = "index")
    public String index;

    @Option(name = "server", shortName = "s")
    public String server;

    @Run
    public void run(String[] args) throws IOException, TException {
        if (args.length != 1) {
            die("Require exactly one query");
        }
        final String query = args[0];
        CommandUtils.openIndexOrConnectToServerAndRun(index, server, new CommandUtils.RpcRunner() {
            @Override
            public void run(TCodeSearchService.Iface rpc) throws TException {
                Preconditions.checkNotNull(rpc);
                TSearchRequest req = new TSearchRequest()
                        .setQuery(query)
                        .setRankFrom(0)
                        .setRankTo(20);
                TSearchResponse resp = rpc.search(req);
                int i = 0;
                for (THit hit : resp.getHits()) {
                    System.out.printf("%d: %d %s/%s (%.2f)\n",
                            i++,
                            hit.getJumpTarget().getFileId(),
                            hit.getProject(),
                            hit.getPath(),
                            hit.getScore());
                    for (String line : Splitter.on("\n").split(hit.getSummary())) {
                        System.out.println("  " + line);
                    }
                }
            }
        });
    }

    @Help
    public void help() {
        System.out.println("Usage: \n" +
                "  polaris search <query> [--index=<dir>] [--server=<ip:port>]\n" +
                "\n" +
                "Options:\n" +
                HelpMessages.INDEX +
                HelpMessages.SERVER +
                "\n");
    }
}
