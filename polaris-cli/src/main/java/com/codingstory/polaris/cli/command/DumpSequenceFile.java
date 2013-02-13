package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.SnappyUtils;
import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;

import java.io.IOException;

import static com.codingstory.polaris.cli.CommandUtils.die;

@Command(name = "seqfile")
public class DumpSequenceFile {

    private static final Log LOG = LogFactory.getLog(DumpSequenceFile.class);
    private static final Configuration LOCAL_CONF = new Configuration();
    private static final FileSystem LOCAL_FS;

    static {
        try {
            LOCAL_FS = LocalFileSystem.getLocal(LOCAL_CONF);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Option(name = "summary", shortName = "s")
    public boolean summary;

    @Run
    public void run(String[] args) throws IOException {
        if (args.length == 0) {
            die("Need more sequence files");
        }
        for (String path : args) {
            doRun(path);
        }
    }

    @Help
    public void help() {
        System.out.println("Usage:\n" +
                "  polaris seqfile [--summary] file1 file2..\n" +
                "\n" +
                "Options:\n" +
                "  -s, --summary    only print summary, default: false\n" +
                "\n");
    }

    private void doRun(String path) throws IOException {
        SequenceFile.Reader reader = null;
        try {
            reader = new SequenceFile.Reader(LOCAL_FS, new Path(path), LOCAL_CONF);
            Class<?> keyClass = reader.getKeyClass();
            Class<?> valueClass = reader.getValueClass();
            Writable key = (Writable) keyClass.newInstance();
            Writable value = (Writable) valueClass.newInstance();
            int count = 0;
            while (reader.next(key, value)) {
                count++;
                if (!summary) {
                    LOG.info("key: " + dumpWritable(key));
                    LOG.info("value: " + dumpWritable(value));
                }
            }
            long size = reader.getPosition();
            LOG.info("File: " + path);
            LOG.info("Records: " + count);
            LOG.info("Avg size: " + size / count);
        } catch (InstantiationException e) {
            throw new AssertionError(e);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private String dumpWritable(Writable value) {
        if (value instanceof BytesWritable) {
            // We only used compressed BytesWritable.
            BytesWritable bw = (BytesWritable) value;
            byte[] b = SnappyUtils.uncompress(bw.getBytes(), 0, bw.getLength());
            // TODO: decode it!
            return "protobuf message of " + b.length + "B";
        } else {
            return value.toString();
        }
    }
}
