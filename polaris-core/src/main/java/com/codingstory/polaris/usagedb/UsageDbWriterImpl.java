package com.codingstory.polaris.usagedb;

import com.codingstory.polaris.SnappyUtils;
import com.codingstory.polaris.parser.ParserProtos.Usage;
import com.codingstory.polaris.usagedb.UsageDbProtos.UsageData;
import com.google.common.base.Preconditions;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;

public class UsageDbWriterImpl implements UsageDbWriter {
    private IndexWriter writer;

    public UsageDbWriterImpl(File path) throws IOException {
        Preconditions.checkNotNull(path);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, new KeywordAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        this.writer = new IndexWriter(FSDirectory.open(path), config);
    }

    @Override
    public void write(Usage usage) throws IOException {
        Preconditions.checkNotNull(usage);
        Document document = new Document();
        long id;
        switch (usage.getKind()) {
            case TYPE:
                id = usage.getType().getType().getClazz().getId();
                break;
            case METHOD:
                id = usage.getMethod().getMethod().getId();
                break;
            case FIELD:
                id = usage.getField().getField().getId();
                break;
            default:
                throw new AssertionError("Unknown kind: " + usage.getKind());
        }
        document.add(new Field(UsageDbIndexedField.ID, String.valueOf(id), Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(UsageDbIndexedField.KIND, String.valueOf(usage.getKind().getNumber()),
                Field.Store.YES, Field.Index.ANALYZED));
        UsageData usageData = UsageData.newBuilder()
                .setUsage(usage)
                .build();
        document.add(new Field(UsageDbIndexedField.USAGE_DATA, SnappyUtils.compress(usageData.toByteArray())));
        writer.addDocument(document);
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
