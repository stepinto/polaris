package com.codingstory.polaris.sourcedb;

import com.codingstory.polaris.SnappyUtils;
import com.codingstory.polaris.indexing.analysis.SourceCodeAnalyzer;
import com.codingstory.polaris.parser.ParserProtos.FileHandle;
import com.codingstory.polaris.parser.ParserProtos.SourceFile;
import com.codingstory.polaris.sourcedb.SourceDbProtos.SourceData;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;

public class SourceDbWriterImpl implements SourceDbWriter {
    private IndexWriter writer;

    public SourceDbWriterImpl(File path) throws IOException {
        Preconditions.checkNotNull(path);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, SourceCodeAnalyzer.getInstance());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        this.writer = new IndexWriter(FSDirectory.open(path), config);
    }
    @Override
    public void writeSourceFile(SourceFile sourceFile) throws IOException {
        Preconditions.checkNotNull(sourceFile);
        Preconditions.checkArgument(
                sourceFile.getHandle().getKind() == FileHandle.Kind.NORMAL_FILE, "Must be NORMAL_FILE");
        Preconditions.checkArgument(!sourceFile.getHandle().getPath().endsWith("/"));
        SourceData sourceData = SourceData.newBuilder()
                .setFileHandle(sourceFile.getHandle())
                .setSourceFile(sourceFile)
                .build();
        doWrite(sourceData);
    }

    private void doWrite(SourceData sourceData) throws IOException {
        Document document = new Document();
        document.add(new LongField(
                SourceDbIndexedField.FILE_ID_RAW,
                sourceData.getFileHandle().getId(),
                Field.Store.YES));
        document.add(new TextField(
                SourceDbIndexedField.PROJECT,
                sourceData.getFileHandle().getProject(),
                Field.Store.YES));
        document.add(new StringField(
                SourceDbIndexedField.PROJECT_RAW,
                sourceData.getFileHandle().getProject(),
                Field.Store.YES));
        document.add(new TextField(
                SourceDbIndexedField.PATH,
                sourceData.getFileHandle().getPath(),
                Field.Store.YES));
        document.add(new StringField(
                SourceDbIndexedField.PATH_RAW,
                sourceData.getFileHandle().getPath(),
                Field.Store.YES));
        document.add(new StringField(
                SourceDbIndexedField.PARENT_PATH_RAW,
                findParentPath(sourceData.getFileHandle().getPath()),
                Field.Store.YES));
        document.add(new TextField(
                SourceDbIndexedField.SOURCE_TEXT,
                sourceData.getSourceFile().getSource(),
                Field.Store.YES));
        byte[] sourceDataBinary = SnappyUtils.compress(sourceData.toByteArray());
        document.add(new StoredField(
                SourceDbIndexedField.SOURCE_DATA,
                new BytesRef(sourceDataBinary)));
        writer.addDocument(document);
    }

    @Override
    public void writeDirectory(FileHandle dir) throws IOException {
        Preconditions.checkNotNull(dir);
        Preconditions.checkArgument(dir.getKind() == FileHandle.Kind.DIRECTORY, "Must be DIRECTORY");
        Preconditions.checkArgument(dir.getPath().endsWith("/"));
        SourceData sourceData = SourceData.newBuilder()
                .setFileHandle(dir)
                .build();
        doWrite(sourceData);
    }

    @Override
    public void flush() throws IOException {
        writer.commit();
        writer.forceMerge(1);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    private String findParentPath(String path) {
        if (Objects.equal(path, "/")) {
            return "";
        } else {
            path = StringUtils.removeEnd(path, "/");
            int lastSlashPos = path.lastIndexOf('/');
            if (lastSlashPos == -1) {
                return "/";
            } else {
                return path.substring(0, lastSlashPos + 1);
            }
        }
    }
}
