package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.SnappyUtils;
import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TList;
import org.apache.thrift.protocol.TType;
import org.apache.thrift.transport.TMemoryInputTransport;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

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
        } catch (TException e) {
            throw new AssertionError(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private String dumpWritable(Writable value) throws TException {
        if (value instanceof BytesWritable) {
            // We only used compressed BytesWritable.
            BytesWritable bw = (BytesWritable) value;
            byte[] b = SnappyUtils.uncompress(bw.getBytes(), 0, bw.getLength());
            TBinaryProtocol proto = new TBinaryProtocol(new TMemoryInputTransport(b));
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            dumpThriftStruct(proto, pw);
            pw.flush();
            pw.print(" " + b.length + "B");
            return sw.toString();
        } else {
            return value.toString();
        }
    }

    private void dumpThriftStruct(TBinaryProtocol proto, PrintWriter out) throws TException {
        out.print("{");
        proto.readStructBegin();
        boolean first = true;
        while (true) {
            TField field = proto.readFieldBegin();
            if (field.type == TType.STOP) {
                break;
            }
            if (!first) {
                out.print(",");
            }
            first = false;
            out.print(field.id);
            out.print(":");
            dumpThriftObject(proto, field.type, out);
            proto.readFieldEnd();
        }
        proto.readStructEnd();
        out.print("}");
    }

    private void dumpThriftObject(TBinaryProtocol proto, int type, PrintWriter out) throws TException {
        switch (type) {
            case TType.BOOL:
                out.print(proto.readBool());
                break;
            case TType.BYTE:
                out.print(StringEscapeUtils.escapeJava(String.valueOf(proto.readByte())));
                break;
            case TType.DOUBLE :
                out.print(proto.readDouble());
                break;
            case TType.I16 :
                out.print(proto.readI16());
                break;
            case TType.I32 :
                out.print(proto.readI32());
                break;
            case TType.I64 :
                out.print(proto.readI64());
                break;
            case TType.STRING :
                String s = StringEscapeUtils.escapeJava(proto.readString());
                if (s.length() > 20) {
                    s = s.substring(0, 20) + "..";
                }
                out.print(s);
                break;
            case TType.STRUCT :
                dumpThriftStruct(proto, out);
                break;
            case TType.LIST:
                dumpThriftList(proto, out);
                break;
            default:
                LOG.warn(type + " is not supported");
                out.print("...");
                break;
        }
    }

    private void dumpThriftList(TBinaryProtocol proto, PrintWriter out) throws TException {
        out.print("[");
        TList list = proto.readListBegin();
        for (int i = 0; i < list.size; i++) {
            if (i > 0) {
                out.print(",");
            }
            dumpThriftObject(proto, list.elemType, out);
        }
        proto.readListEnd();
        out.print("]");
    }
}
