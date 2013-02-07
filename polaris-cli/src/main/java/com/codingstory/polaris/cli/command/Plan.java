package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import com.codingstory.polaris.pipeline.IndexPipeline;
import com.sun.org.apache.commons.logging.Log;
import com.sun.org.apache.commons.logging.LogFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

@Command(name = "plan")
public class Plan {

    private static final Log LOG = LogFactory.getLog(Plan.class);

    @Option(name = "out", shortName = "o", defaultValue = "plan.dot")
    public String outPath;

    @Run
    public void run(String[] args) throws IOException {
        IndexPipeline pipeline = new IndexPipeline();
        try {
            String plan = pipeline.plan();
            FileUtils.write(new File(outPath), plan);
            LOG.info("Plan is saved to " + outPath);
        } finally {
            pipeline.cleanUp();
        }
    }

    @Help
    public void help() {
        System.out.println("Usage:\n" +
                "  polaris plan [--out FILE]\n\n");
    }
}
