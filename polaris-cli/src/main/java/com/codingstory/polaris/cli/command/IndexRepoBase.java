package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import com.codingstory.polaris.pipeline.IndexPipeline;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static com.codingstory.polaris.cli.CommandUtils.die;

@Command(name = "indexrepobase")
public class IndexRepoBase {
    @Option(name = "index", shortName = "i", defaultValue = "index")
    public String index;

    @Run
    public void run(String[] args) throws IOException {
        if (args.length != 1) {
            die("Require repobase dir");
        }
        File indexDir = new File(index);
        FileUtils.deleteQuietly(indexDir);
        FileUtils.forceMkdir(indexDir);
        IndexPipeline pipeline = new IndexPipeline();
        pipeline.addRepoBase(new File(args[0]));
        pipeline.run();
        pipeline.cleanUp();
    }

    @Help
    public void help() {
        System.out.println("Usage:\n" +
                "  polaris indexrepobase [--clean] [--index=<index-dir>] <repobase-dir>\n" +
                "\n" +
                "Options:\n" +
                "  -i, --index          output index directory: default: ./index\n" +
                "\n");
    }
}
