package com.codingstory.polaris.usagedb;

import com.codingstory.polaris.SnappyUtils;
import com.codingstory.polaris.parser.TypeHandle;
import com.codingstory.polaris.parser.TypeUsage;
import com.google.common.base.Preconditions;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.io.File;
import java.io.IOException;

public class UsageDbWriterImpl implements UsageDbWriter {
    private static final TSerializer SERIALIZER = new TSerializer(new TBinaryProtocol.Factory());
    private IndexWriter writer;

    public UsageDbWriterImpl(File path) throws IOException {
        Preconditions.checkNotNull(path);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, new KeywordAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        this.writer = new IndexWriter(FSDirectory.open(path), config);
    }

    @Override
    public void write(TypeUsage usage) throws IOException {
        Preconditions.checkNotNull(usage);
        TypeHandle handle = usage.getType();
        Preconditions.checkNotNull(handle);
        Preconditions.checkArgument(handle.isResolved());
        try {
            Document document = new Document();
            document.add(new Field(UsageDbIndexedField.TYPE_ID_RAW, String.valueOf(handle.getId()),
                    Field.Store.YES, Field.Index.ANALYZED));
            TUsageData usageData = new TUsageData();
            usageData.setTypeUsage(usage.toThrift());
            byte[] usageDataBinary = SERIALIZER.serialize(usageData);
            document.add(new Field(UsageDbIndexedField.USAGE_DATA, SnappyUtils.compress(usageDataBinary)));
            writer.addDocument(document);
        } catch (TException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void flush() throws IOException {
        writer.commit();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
