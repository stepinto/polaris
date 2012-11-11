package com.codingstory.polaris.indexing;

import com.codingstory.polaris.parser.ClassType;
import com.google.common.base.Preconditions;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexWriter;
import org.xerial.snappy.Snappy;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.codingstory.polaris.indexing.FieldName.DIRECTORY_NAME;
import static com.codingstory.polaris.indexing.FieldName.FILE_CONTENT;
import static com.codingstory.polaris.indexing.FieldName.FILE_ID;
import static com.codingstory.polaris.indexing.FieldName.FILE_NAME;
import static com.codingstory.polaris.indexing.FieldName.KIND;
import static com.codingstory.polaris.indexing.FieldName.OFFSET;
import static com.codingstory.polaris.indexing.FieldName.PROJECT_NAME;
import static com.codingstory.polaris.indexing.FieldName.SOURCE_ANNOTATIONS;
import static com.codingstory.polaris.indexing.FieldName.TYPE_FULL_NAME_RAW;
import static com.codingstory.polaris.indexing.FieldName.TYPE_NAME;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-17
 * Time: 下午7:21
 * To change this template use File | Settings | File Templates.
 */
public class JavaIndexer {

    private static Log LOG = LogFactory.getLog(JavaIndexer.class);
    private final IndexWriter writer;
    private final String projectName;

    public JavaIndexer(IndexWriter writer, String projectName) {
        this.writer = Preconditions.checkNotNull(writer);
        this.projectName = Preconditions.checkNotNull(projectName);
    }

    private void addIndexFieldToDocument(Document document, String fieldName, String content) {
        Fieldable f = new Field(
                fieldName,
                content,
                Field.Store.YES,
                Field.Index.ANALYZED,
                Field.TermVector.WITH_POSITIONS_OFFSETS);
        f.setBoost(2.0f);
        document.add(f);
    }

    /**
     * Indexes file content.
     */
    public void indexFile(String filePath, byte[] content, String annotatedSource) throws IOException {
        Preconditions.checkNotNull(filePath);
        Preconditions.checkNotNull(content);
        Preconditions.checkNotNull(annotatedSource);
        byte[] sha1sum = DigestUtils.sha(content);
        LOG.debug("Indexing file content: " + projectName + filePath);
        Document document = new Document();
        document.add(new Field(FILE_ID, Hex.encodeHexString(sha1sum), Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field(FILE_CONTENT, Snappy.compress(content)));
        document.add(new Field(PROJECT_NAME, projectName, Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field(FILE_NAME, filePath, Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field(SOURCE_ANNOTATIONS, Snappy.compress(annotatedSource)));
        document.add(new Field(DIRECTORY_NAME, getParentPath(filePath), Field.Store.YES, Field.Index.NOT_ANALYZED));
        writer.addDocument(document);
    }
    public void indexTypes(String filePath, byte[] content, List<ClassType> types) throws IOException {
        byte[] sha1sum = DigestUtils.sha(content);
        for (ClassType type : types) {
            indexType(filePath, sha1sum, type);
        }
    }

    private void indexType(String filePath, byte[] sha1sum, ClassType type) throws IOException {
        Preconditions.checkNotNull(projectName);
        Preconditions.checkNotNull(filePath);
        Preconditions.checkNotNull(type);
        String typeName = type.getName().toString();
        Document document = new Document();
        document.add(new Field(PROJECT_NAME, projectName, Field.Store.YES, Field.Index.NO));
        document.add(new Field(FILE_NAME, filePath, Field.Store.YES, Field.Index.NO));
        document.add(new Field(FILE_ID, Hex.encodeHexString(sha1sum), Field.Store.YES, Field.Index.NO));
        document.add(new Field(OFFSET, String.valueOf(type.getJumpTarget().getOffset()), Field.Store.YES, Field.Index.NO));
        document.add(new Field(KIND, String.valueOf(type.getKind().ordinal()), Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field(TYPE_NAME, typeName, Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(TYPE_FULL_NAME_RAW, typeName, Field.Store.YES,  Field.Index.ANALYZED));
        // TODO: process fields/methods
        writer.addDocument(document);
    }

    private static String getParentPath(String path) {
        Preconditions.checkNotNull(path);
        int lastSlash = path.lastIndexOf(File.separatorChar);
        Preconditions.checkArgument(lastSlash != -1, "Bad path: " + path);
        return path.substring(0, lastSlash);
    }
}
