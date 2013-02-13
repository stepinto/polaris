package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.NoOpController;
import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import com.codingstory.polaris.search.CodeSearchImpl;
import com.codingstory.polaris.search.SearchProtos.SourceRequest;
import com.codingstory.polaris.search.SearchProtos.SourceResponse;
import org.apache.commons.io.IOUtils;

import java.io.File;
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
    public void run(String[] args) throws IOException {
        if (args.length != 1) {
            die("Require exactly one file-id");
        }
        final long fileId = Long.parseLong(args[0]);
        CodeSearchImpl searcher = new CodeSearchImpl(new File(index));
        try {
            SourceRequest req = SourceRequest.newBuilder()
                    .setFileId(fileId)
                    .build();
            SourceResponse resp = searcher.source(NoOpController.getInstance(), req);
            checkStatus(resp.getStatus());
            if (annotated) {
                System.out.println(resp.getSource().getAnnotatedSource());
            } else {
                System.out.println(resp.getSource().getSource());
            }
        } finally {
            IOUtils.closeQuietly(searcher);
        }
    }

    @Help
    public void help() {
        System.out.println("Usage: \n" +
                "  polaris source <file-id> [--index=<dir>] [--server=<ip:port>] [--annotated]\n" +
                "\n" +
                "Options:\n" +
                HelpMessages.INDEX +
                "  -a, --annotated      display annotated source, default: false\n" +
                "\n");
    }
}
