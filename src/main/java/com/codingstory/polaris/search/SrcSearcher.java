package com.codingstory.polaris.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-18
 * Time: 下午11:43
 * To change this template use File | Settings | File Templates.
 */
public class SrcSearcher {
    final IndexReader reader;
    final IndexSearcher searcher;
    final QueryParser parser;

    public SrcSearcher(String indexDirectory) throws IOException {
        reader = IndexReader.open(FSDirectory.open(new File(indexDirectory)));
        searcher = new IndexSearcher(reader);
        parser = new QueryParser(Version.LUCENE_36, "content", new WhitespaceAnalyzer(Version.LUCENE_36));
    }

    public void close() throws IOException {
        searcher.close();
        reader.close();
    }

    public List<Result> search(String queryString, int limit) throws ParseException, IOException, InvalidTokenOffsetsException {
        Query query = parser.parse(queryString);
        TopDocs topDocs = searcher.search(query, limit);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        List<Result> results = new ArrayList<Result>();
        Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(), new QueryScorer(query));
        Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_36);
        for (ScoreDoc doc : scoreDocs) {
            Result result = new Result();
            int docid = doc.doc;
            Document document = reader.document(docid);
            result.setFilename(document.getFieldable("filename").stringValue());
            String content = document.getFieldable("content").stringValue();
            result.setContent(content);
            result.setSummary(getSummary(content, queryString));
            results.add(result);
        }
        return results;
    }

    public String getSummary(String content, String query) {
        String[] queryParts = query.split("\\s+");
        String[] lines = content.split("\n");
        int i = 0;
        outer:
        for (; i < lines.length; ++i) {
            for (String term : queryParts) {
                if (lines[i].contains(term)) {
                    break outer;
                }
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int j = i - 2; j < i + 3; ++j) {
            if (j >= 0 && j < lines.length) {
                builder.append(lines[j]);
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    public static void main(String[] args) throws Exception {
        SrcSearcher searcher = new SrcSearcher("index");
        List<Result> results = searcher.search("toString", 3);
        for (Result result : results) {
            System.out.println(result);
        }
        searcher.close();
    }
}
