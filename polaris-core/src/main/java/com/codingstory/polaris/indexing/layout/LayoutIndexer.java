package com.codingstory.polaris.indexing.layout;

import com.google.common.base.Preconditions;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.io.File;
import java.io.IOException;

import static com.codingstory.polaris.indexing.FieldName.DIRECTORY_NAME;
import static com.codingstory.polaris.indexing.FieldName.DIRECTORY_LAYOUT;
import static com.codingstory.polaris.indexing.FieldName.PROJECT_NAME;

/** Indexes project layout. */
public class LayoutIndexer {

    private static final TSerializer SERIALIZER = new TSerializer(new TBinaryProtocol.Factory());
    private final IndexWriter indexWriter;
    private final String projectName;

    public LayoutIndexer(IndexWriter indexWriter, String projectName) {
        this.indexWriter = Preconditions.checkNotNull(indexWriter);
        this.projectName = Preconditions.checkNotNull(projectName);
    }

    public void indexDirectory(File dir) throws IOException {
        Preconditions.checkNotNull(dir);
        Preconditions.checkArgument(dir.isDirectory(), "Bad directory: " + dir);

        File[] children = dir.listFiles();
        if (children == null) {
            throw new IOException("Bad directory: " + dir);
        }
        TLayoutNodeList layout = new TLayoutNodeList();
        for (File child : children) {
            TLayoutNode node = new TLayoutNode();
            if (child.isDirectory()) {
                node.setKind(TLayoutNodeKind.DIRECTORY);
            } else {
                node.setKind(TLayoutNodeKind.FILE);
            }
            node.setName(child.getName());
            layout.addToNodes(node);
        }

        Document document = new Document();
        document.add(new Field(PROJECT_NAME, projectName, Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field(DIRECTORY_NAME, dir.getName(), Field.Store.YES,  Field.Index.NOT_ANALYZED));
        try {
            document.add(new Field(DIRECTORY_LAYOUT, SERIALIZER.serialize(layout)));
        } catch (TException e) {
            throw new AssertionError(e);
        }
        indexWriter.addDocument(document);
    }
}
