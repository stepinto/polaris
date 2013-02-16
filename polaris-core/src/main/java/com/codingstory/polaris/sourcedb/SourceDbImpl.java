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
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
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
    public DirectoryContent listDirectory(String project, String path) throws IOException {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(path);
        path = SourceDbUtils.fixPathForDirectory(path);
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(new TermQuery(new Term(SourceDbIndexedField.PROJECT_RAW, project)), BooleanClause.Occur.MUST);
        booleanQuery.add(new TermQuery(new Term(SourceDbIndexedField.PARENT_PATH_RAW, path)), BooleanClause.Occur.MUST);
        TopDocs topDocs = searcher.search(booleanQuery, Integer.MAX_VALUE);
        List<String> dirs = Lists.newArrayList();
        List<FileHandle> files = Lists.newArrayList();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            int docId = scoreDoc.doc;
            Document document = reader.document(docId);
            if (document.get(SourceDbIndexedField.FILE_ID_RAW) != null) {
                files.add(FileHandle.newBuilder()
                        .setId(Long.parseLong(document.get(SourceDbIndexedField.FILE_ID_RAW)))
                        .setProject(document.get(SourceDbIndexedField.PROJECT_RAW))
                        .setPath(document.get(SourceDbIndexedField.PATH_RAW))
                        .build());
            } else {
                dirs.add(document.get(SourceDbIndexedField.PATH_RAW));
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
        TermQuery query = new TermQuery(new Term(SourceDbIndexedField.FILE_ID_RAW, String.valueOf(fileId)));
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
        return retrieveDocument(topDocs.scoreDocs[0].doc);
    }

    @Override
    public List<Hit> query(String query, int n) throws IOException {
        Preconditions.checkNotNull(query);
        Preconditions.checkArgument(n >= 0);
        try {
            MultiFieldQueryParser parser = new MultiFieldQueryParser(
                    Version.LUCENE_36,
                    SEARCHABLE_FIELDS,
                    SourceCodeAnalyzer.getInstance());
            TopDocs topDocs = searcher.search(parser.parse(query), n);
            List<Hit> hits = Lists.newArrayList();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                SourceFile source = retrieveDocument(scoreDoc.doc);
                JumpTarget jumpTarget = JumpTarget.newBuilder()
                        .setFile(source.getHandle())
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

    private SourceFile retrieveDocument(int docId) throws IOException {
        Document document = reader.document(docId);
        SourceData sourceData = SourceData.parseFrom(
                SnappyUtils.uncompress(document.getBinaryValue(SourceDbIndexedField.SOURCE_DATA)));
        return sourceData.getSourceFile();
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(reader);
        IOUtils.closeQuietly(searcher);
    }
}
