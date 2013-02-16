package com.codingstory.polaris.sourcedb;

import com.codingstory.polaris.SnappyUtils;
import com.codingstory.polaris.indexing.analysis.SourceCodeAnalyzer;
import com.codingstory.polaris.parser.ParserProtos.SourceFile;
import com.codingstory.polaris.sourcedb.SourceDbProtos.SourceData;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;

public class SourceDbWriterImpl implements SourceDbWriter {
    private IndexWriter writer;

    public SourceDbWriterImpl(File path) throws IOException {
        Preconditions.checkNotNull(path);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, SourceCodeAnalyzer.getInstance());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        this.writer = new IndexWriter(FSDirectory.open(path), config);
    }
    @Override
    public void writeSourceFile(SourceFile sourceFile) throws IOException {
        Preconditions.checkNotNull(sourceFile);
        Document document = new Document();
        document.add(new Field(SourceDbIndexedField.FILE_ID_RAW, String.valueOf(sourceFile.getHandle().getId()),
                Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(SourceDbIndexedField.PROJECT, sourceFile.getHandle().getProject(),
                Field.Store.YES,  Field.Index.ANALYZED));
        document.add(new Field(SourceDbIndexedField.PROJECT_RAW, sourceFile.getHandle().getProject(),
                Field.Store.YES,  Field.Index.ANALYZED));
        document.add(new Field(SourceDbIndexedField.PATH, sourceFile.getHandle().getPath(),
                Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(SourceDbIndexedField.PATH_RAW, sourceFile.getHandle().getPath(),
                Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(SourceDbIndexedField.PARENT_PATH_RAW,
                findParentPath(sourceFile.getHandle().getPath()), Field.Store.YES, Field.Index.ANALYZED));
        SourceData sourceData = SourceData.newBuilder()
                .setSourceFile(sourceFile)
                .build();
        byte[] sourceDataBinary = SnappyUtils.compress(sourceData.toByteArray());
        document.add(new Field(SourceDbIndexedField.SOURCE_DATA, sourceDataBinary));
        document.add(new Field(SourceDbIndexedField.SOURCE_TEXT,
                sourceData.getSourceFile().getSource(), Field.Store.YES,
                Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
        writer.addDocument(document);
    }

    @Override
    public void writeDirectory(String project, String path) throws IOException {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(path);
        path = SourceDbUtils.fixPathForDirectory(path);
        Document document = new Document();
        document.add(new Field(SourceDbIndexedField.PROJECT_RAW, project, Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(SourceDbIndexedField.PATH_RAW, path, Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(SourceDbIndexedField.PARENT_PATH_RAW, findParentPath(path),
                Field.Store.YES, Field.Index.ANALYZED));
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
