package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import com.codingstory.polaris.indexing.IndexBuilder;
import com.codingstory.polaris.repo.GitUtils;
import com.codingstory.polaris.repo.Repository;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static com.codingstory.polaris.cli.CommandUtils.createIndexer;
import static com.codingstory.polaris.cli.CommandUtils.die;

@Command(name = "indexrepobase")
public class IndexRepoBase {
    @Option(name = "clean", shortName = "c")
    public boolean clean;

    @Option(name = "index", shortName = "i", defaultValue = "index")
    public String index;

    @Run
    public void run(String[] args) throws IOException {
        if (args.length != 1) {
            die("Require repobase dir");
        }
        File indexDir = new File(index);
        if (clean) {
            FileUtils.deleteDirectory(indexDir);
        }
        IndexBuilder indexer = createIndexer(indexDir);
        for (Repository repo : GitUtils.openRepoBase(new File(args[0]))) {
            indexer.indexRepository(repo);
        }
    }

    @Help
    public void help() {
        System.out.println("Usage:\n" +
                "  polaris indexrepobase [--clean] [--index=<index-dir>] <repobase-dir>\n" +
                "\n" +
                "Options:\n" +
                "  -c, --clean          remove any existing index files, default: false\n" +
                "  -i, --index          output index directory: default: ./index\n" +
                "\n");
    }
}
