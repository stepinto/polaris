package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.NoOpController;
import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import com.codingstory.polaris.search.CodeSearchImpl;
import com.codingstory.polaris.parser.ParserProtos.FileHandle;
import com.codingstory.polaris.search.SearchProtos.Hit;
import com.codingstory.polaris.search.SearchProtos.SearchRequest;
import com.codingstory.polaris.search.SearchProtos.SearchResponse;
import com.google.common.base.Splitter;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;

import static com.codingstory.polaris.cli.CommandUtils.die;

@Command(name = "search")
public class Search {
    @Option(name = "index", shortName = "i", defaultValue = "index")
    public String index;

    @Run
    public void run(String[] args) throws IOException {
        if (args.length != 1) {
            die("Require exactly one query");
        }
        final String query = args[0];
        CodeSearchImpl searcher = new CodeSearchImpl(new File(index));
        try {
            SearchRequest req = SearchRequest.newBuilder()
                    .setQuery(query)
                    .setRankFrom(0)
                    .setRankTo(20)
                    .build();
            SearchResponse resp = searcher.search(NoOpController.getInstance(), req);
            int i = 0;
            for (Hit hit : resp.getHitsList()) {
                FileHandle file = hit.getJumpTarget().getFile();
                System.out.printf("%d: %d %s/%s (%.2f)\n",
                        i++,
                        file.getId(),
                        file.getProject(),
                        file.getPath(),
                        hit.getScore());
                for (String line : Splitter.on("\n").split(hit.getSummary())) {
                    System.out.println("  " + line);
                }
            }
        }
        finally {
            IOUtils.closeQuietly(searcher);
        }
    }

    @Help
    public void help() {
        System.out.println("Usage: \n" +
                "  polaris search <query> [--index=<dir>] [--server=<ip:port>]\n" +
                "\n" +
                "Options:\n" +
                HelpMessages.INDEX +
                "\n");
    }
}
