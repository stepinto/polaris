package com.codingstory.polaris.indexing;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-16
 * Time: 下午10:52
 * Sample Indexer to show usage of lucene
 */
public class SampleIndexer {
    public static void main(String[] args) throws Exception {
        // create index
        Directory dir = FSDirectory.open(new File("index"));
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(dir, config);
        Document document = new Document();
        document.add(new Field("test_field", "hello world", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(document);
        writer.close();
        dir.close();

        // search
        IndexReader reader = IndexReader.open(FSDirectory.open(new File("index")));
        IndexSearcher searcher = new IndexSearcher(reader);
        QueryParser parser = new QueryParser(Version.LUCENE_43, "test_field", analyzer);
        Query q = parser.parse("hello");
        TopDocs docs = searcher.search(q, null, 100);
        ScoreDoc[] results = docs.scoreDocs;
        for (ScoreDoc doc : results) {
            System.out.println(searcher.doc(doc.doc).get("test_field"));
        }
        reader.close();
    }
}
