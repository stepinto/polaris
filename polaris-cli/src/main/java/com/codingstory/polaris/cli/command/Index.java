package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import com.codingstory.polaris.indexing.IndexBuilder;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static com.codingstory.polaris.cli.CommandUtils.createIndexer;
import static com.codingstory.polaris.cli.CommandUtils.die;

@Command(name = "index")
public class Index {
    @Option(name = "clean", shortName = "c")
    public boolean clean;

    @Option(name = "index", shortName = "i", defaultValue = "index")
    public String index;

    @Run
    public void run(String[] args) throws IOException {
        if (args.length == 0) {
            die("Expect one or more projects to index");
        }
        File indexDir = new File(index);
        if (clean) {
            FileUtils.deleteDirectory(indexDir);
        }
        IndexBuilder indexer = createIndexer(indexDir);
        for (String arg : args) {
            indexer.indexDirectory(new File(arg));
        }
    }

    @Help
    public void help() {
        System.out.println("Usage:\n" +
                "  polaris index [--clean] [--index=<index-dir>] project1 project2..\n" +
                "\n" +
                "Options:\n" +
                "  -c, --clean          remove any existing index files, default: false\n" +
                "  -i, --index          output index directory: default: ./index\n" +
                "\n");
    }
}
