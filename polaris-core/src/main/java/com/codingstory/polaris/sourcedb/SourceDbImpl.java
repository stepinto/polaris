package com.codingstory.polaris.sourcedb;

import com.codingstory.polaris.parser.FileHandle;
import com.codingstory.polaris.parser.SourceFile;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.xerial.snappy.Snappy;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SourceDbImpl implements SourceDb {
    private static final Log LOG = LogFactory.getLog(SourceDbImpl.class);
    private static final TDeserializer DESERIALIZER = new TDeserializer(new TBinaryProtocol.Factory());
    private final IndexReader reader;
    private final IndexSearcher searcher;

    public SourceDbImpl(File path) throws IOException {
        Preconditions.checkNotNull(path);
        reader = IndexReader.open(FSDirectory.open(path));
        searcher = new IndexSearcher(reader);
    }
    @Override
    public DirectoryContent listDirectory(String project, String path) throws IOException {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(path);
        path = SourceDbUtils.fixPathForDirectory(path);
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(new TermQuery(new Term(SourceDbIndxedField.PROJECT_RAW, project)), BooleanClause.Occur.MUST);
        booleanQuery.add(new TermQuery(new Term(SourceDbIndxedField.PARENT_PATH_RAW, path)), BooleanClause.Occur.MUST);
        TopDocs topDocs = searcher.search(booleanQuery, Integer.MAX_VALUE);
        List<String> dirs = Lists.newArrayList();
        List<FileHandle> files = Lists.newArrayList();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            int docId = scoreDoc.doc;
            Document document = reader.document(docId);
            if (document.get(SourceDbIndxedField.FILE_ID_RAW) != null) {
                files.add(new FileHandle(
                        Long.parseLong(document.get(SourceDbIndxedField.FILE_ID_RAW)),
                        document.get(SourceDbIndxedField.PROJECT_RAW),
                        document.get(SourceDbIndxedField.PATH_RAW)));
            } else {
                dirs.add(document.get(SourceDbIndxedField.PATH_RAW));
            }
        }
        Collections.sort(dirs);
        Collections.sort(files, new Comparator<FileHandle>() {
            @Override
            public int compare(FileHandle left, FileHandle right) {
                return left.getPath().compareTo(right.getPath());
            }
        });
        return new DirectoryContent(dirs, files);
    }

    @Override
    public SourceFile querySourceById(long fileId) throws IOException {
        TermQuery query = new TermQuery(new Term(SourceDbIndxedField.FILE_ID_RAW, String.valueOf(fileId)));
        TopDocs topDocs = searcher.search(query, 2);
        int count = topDocs.scoreDocs.length;
        if (count == 0) {
            LOG.debug("Source not found: " + fileId);
            return null;
        }
        if (count > 1) {
            LOG.warn("Ambiguous file id: " + fileId);
        }
        return retrieveDocument(topDocs.scoreDocs[0].doc);
    }

    @Override
    public SourceFile querySourceByPath(String project, String path) throws IOException {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(path);
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(new TermQuery(new Term(SourceDbIndxedField.PROJECT_RAW, project)), BooleanClause.Occur.MUST);
        booleanQuery.add(new TermQuery(new Term(SourceDbIndxedField.PATH_RAW, path)), BooleanClause.Occur.MUST);
        TopDocs topDocs = searcher.search(booleanQuery, 2);
        int count = topDocs.scoreDocs.length;
        if (count == 0) {
            LOG.debug("Source not found: " + project + path);
            return null;
        } else if (count > 1) {
            throw new IOException("Ambiguous project and path: " + project + path);
        }
        return retrieveDocument(topDocs.scoreDocs[0].doc);
    }

    private SourceFile retrieveDocument(int docId) throws IOException {
        try {
            Document document = reader.document(docId);
            byte[] binaryData = document.getBinaryValue(SourceDbIndxedField.SOURCE_DATA);
            TSourceData sourceData = new TSourceData();
            DESERIALIZER.deserialize(sourceData, Snappy.uncompress(binaryData));
            return SourceFile.createFromThrift(sourceData.getSourceFile());
        } catch (TException e) {
            throw new IOException(e);
        }
    }
}
