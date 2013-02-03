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

@Command(name = "index")
public class Index {
    @Option(name = "index", shortName = "i", defaultValue = "index")
    public String index;

    @Run
    public void run(String[] args) throws IOException {
        if (args.length == 0) {
            die("Expect one or more projects to index");
        }
        File indexDir = new File(index);
        FileUtils.deleteQuietly(indexDir);
        FileUtils.forceMkdir(indexDir);
        IndexPipeline pipeline = new IndexPipeline();
        for (String arg : args) {
            pipeline.addProjectDirectory(new File(arg));
        }
        pipeline.setIndexDirectory(indexDir);
        pipeline.run();
        pipeline.cleanUp();
    }

    @Help
    public void help() {
        System.out.println("Usage:\n" +
                "  polaris index [--clean] [--index=<index-dir>] project1 project2..\n" +
                "\n" +
                "Options:\n" +
                "  -i, --index          output index directory: default: ./index\n" +
                "\n");
    }
}
