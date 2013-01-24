package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.CommandUtils;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import com.codingstory.polaris.search.TCodeSearchService;
import com.codingstory.polaris.search.TSourceRequest;
import com.codingstory.polaris.search.TSourceResponse;
import org.apache.thrift.TException;

import java.io.IOException;

import static com.codingstory.polaris.cli.CommandUtils.checkStatus;
import static com.codingstory.polaris.cli.CommandUtils.die;

@Command(name = "source")
public class Source {
    @Option(name = "index", shortName = "i", defaultValue = "index")
    public String index;

    @Option(name = "server", shortName = "s")
    public String server;

    @Option(name = "annotated", shortName = "a")
    public boolean annotated;

    @Run
    public void run(String[] args) throws TException, IOException {
        if (args.length != 1) {
            die("Require exactly one file-id");
        }
        final long fileId = Long.parseLong(args[0]);
        CommandUtils.openIndexOrConnectToServerAndRun(index, server, new CommandUtils.RpcRunner() {
            @Override
            public void run(TCodeSearchService.Iface rpc) throws TException {
                TSourceRequest req = new TSourceRequest();
                req.setFileId(fileId);
                TSourceResponse resp = rpc.source(req);
                checkStatus(resp.getStatus());
                if (annotated) {
                    System.out.println(resp.getSource().getAnnotatedSource());
                } else {
                    System.out.println(resp.getSource().getSource());
                }
            }
        });
    }

    @Help
    public void help() {
        System.out.println("Usage: \n" +
                "  polaris source <file-id> [--index=<dir>] [--server=<ip:port>] [--annotated]\n" +
                "\n" +
                "Options:\n" +
                HelpMessages.INDEX +
                HelpMessages.SERVER +
                "  -a, --annotated      display annotated source, default: false\n" +
                "\n");
    }
}
