package com.codingstory.polaris.sourcedb;

import com.codingstory.polaris.SnappyUtils;
import com.codingstory.polaris.indexing.analysis.SourceCodeAnalyzer;
import com.codingstory.polaris.parser.ParserProtos.FileHandle;
import com.codingstory.polaris.parser.ParserProtos.JumpTarget;
import com.codingstory.polaris.parser.ParserProtos.SourceFile;
import com.codingstory.polaris.search.SearchProtos.Hit;
import com.codingstory.polaris.sourcedb.SourceDbProtos.SourceData;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SourceDbImpl implements SourceDb {
    private static final Log LOG = LogFactory.getLog(SourceDbImpl.class);
    private final IndexReader reader;
    private final IndexSearcher searcher;
    private static final String[] SEARCHABLE_FIELDS = {
            SourceDbIndexedField.PROJECT,
            SourceDbIndexedField.PATH,
            SourceDbIndexedField.SOURCE_TEXT
    };

    public SourceDbImpl(File path) throws IOException {
        Preconditions.checkNotNull(path);
        reader = IndexReader.open(FSDirectory.open(path));
        searcher = new IndexSearcher(reader);
    }
    @Override
    public List<FileHandle> listDirectory(String project, String path) throws IOException {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(path);
        path = SourceDbUtils.fixPathForDirectory(path);
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(new TermQuery(new Term(SourceDbIndexedField.PROJECT_RAW, project)), BooleanClause.Occur.MUST);
        booleanQuery.add(new TermQuery(new Term(SourceDbIndexedField.PARENT_PATH_RAW, path)), BooleanClause.Occur.MUST);
        TopDocs topDocs = searcher.search(booleanQuery, Integer.MAX_VALUE);
        List<FileHandle> children = Lists.newArrayList();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            children.add(retrieveDocument(scoreDoc.doc).getFileHandle());
        }
        sortFileHandlesByPath(children);
        return children;
    }

    private void sortFileHandlesByPath(List<FileHandle> children) {
        Collections.sort(children, new Comparator<FileHandle>() {
            @Override
            public int compare(FileHandle left, FileHandle right) {
                return left.getPath().compareTo(right.getPath());
            }
        });
    }

    @Override
    public SourceFile querySourceById(long fileId) throws IOException {
        System.out.println("SourceDbImpl.querySourceById");
        Query query = NumericRangeQuery.newLongRange(SourceDbIndexedField.FILE_ID_RAW, fileId, fileId, true, true);
        TopDocs topDocs = searcher.search(query, 2);
        System.out.println("topDocs.scoreDocs.length = " + topDocs.scoreDocs.length);
        int count = topDocs.scoreDocs.length;
        if (count == 0) {
            LOG.debug("Source not found: " + fileId);
            return null;
        }
        if (count > 1) {
            LOG.warn("Ambiguous file id: " + fileId);
        }
        return retrieveDocumentAsNormalFile(topDocs.scoreDocs[0].doc);
    }

    @Override
    public SourceFile querySourceByPath(String project, String path) throws IOException {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(path);
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(new TermQuery(new Term(SourceDbIndexedField.PROJECT_RAW, project)), BooleanClause.Occur.MUST);
        booleanQuery.add(new TermQuery(new Term(SourceDbIndexedField.PATH_RAW, path)), BooleanClause.Occur.MUST);
        TopDocs topDocs = searcher.search(booleanQuery, 2);
        int count = topDocs.scoreDocs.length;
        if (count == 0) {
            LOG.debug("Source not found: " + project + path);
            return null;
        } else if (count > 1) {
            throw new IOException("Ambiguous project and path: " + project + path);
        }
        return retrieveDocumentAsNormalFile(topDocs.scoreDocs[0].doc);
    }

    @Override
    public FileHandle getFileHandle(String project, String path) throws IOException {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(path);
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(new TermQuery(new Term(SourceDbIndexedField.PROJECT_RAW, project)), BooleanClause.Occur.MUST);
        booleanQuery.add(new TermQuery(new Term(SourceDbIndexedField.PATH_RAW, path)), BooleanClause.Occur.MUST);
        TopDocs topDocs = searcher.search(booleanQuery, 2);
        int count = topDocs.scoreDocs.length;
        if (count == 0) {
            LOG.debug("File not found: " + project + path);
            return null;
        } else if (count > 1) {
            throw new IOException("Ambiguous project and path: " + project + path);
        }
        return retrieveDocument(topDocs.scoreDocs[0].doc).getFileHandle();
    }

    @Override
    public List<Hit> query(String query, int n) throws IOException {
        Preconditions.checkNotNull(query);
        Preconditions.checkArgument(n >= 0);
        try {
            MultiFieldQueryParser parser = new MultiFieldQueryParser(
                    Version.LUCENE_43,
                    SEARCHABLE_FIELDS,
                    SourceCodeAnalyzer.getInstance());
            Query parsedQuery = parser.parse(query);
            TopDocs topDocs = searcher.search(parsedQuery, n);
            List<Hit> hits = Lists.newArrayList();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                SourceData data = retrieveDocument(scoreDoc.doc);
                if (data.getFileHandle().getKind() != FileHandle.Kind.NORMAL_FILE) {
                    continue; // Ignore directories.
                }
                JumpTarget jumpTarget = JumpTarget.newBuilder()
                        .setFile(data.getFileHandle())
                        .build();
                hits.add(Hit.newBuilder()
                        .setKind(Hit.Kind.FILE)
                        .setQueryHint(getFileName(jumpTarget.getFile().getPath()))
                        .setJumpTarget(jumpTarget)
                        .setScore(scoreDoc.score)
                        .setSummary("TODO")
                        .build());
            }
            return hits;
        } catch (ParseException e) {
            LOG.info("Query syntax error: " + query);
            return ImmutableList.of();
        }
    }

    private String getFileName(String path) {
        int slash = path.lastIndexOf("/");
        if (slash == -1) {
            return path;
        }
        return path.substring(slash + 1);
    }

    private SourceData retrieveDocument(int docId) throws IOException {
        Document document = reader.document(docId);
        BytesRef bytesRef = document.getBinaryValue(SourceDbIndexedField.SOURCE_DATA);
        return SourceData.parseFrom(SnappyUtils.uncompress(bytesRef.bytes, bytesRef.offset, bytesRef.length));
    }

    private SourceFile retrieveDocumentAsNormalFile(int docId) throws IOException {
        SourceData data = retrieveDocument(docId);
        if (data.getFileHandle().getKind() != FileHandle.Kind.NORMAL_FILE) {
            throw new IOException("The result is not NORMAL_FILE: " + data.getFileHandle());
        }
        return data.getSourceFile();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
