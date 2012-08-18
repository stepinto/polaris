package com.codingstory.polaris.indexing;

import com.codingstory.polaris.indexing.analysis.JavaSrcAnalyzer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-17
 * Time: 下午7:21
 * To change this template use File | Settings | File Templates.
 */
public class DirectoryIndexer {
    public static void buildIndex(String rootdir, IndexWriter writer) throws IOException {
        Log LOG = LogFactory.getLog(DirectoryIndexer.class);
        DirectoryTranverser tranverser = new DirectoryTranverser(rootdir);
        File f;
        while ((f = tranverser.getNextFile()) != null) {
            if (!f.getName().endsWith(".java")) {
                continue;
            }
            LOG.info("indexing " + f.getAbsolutePath());
            String fileContent = FileUtils.readFileToString(f);
            Document document = new Document();
            document.add(new Field("content", fileContent, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
            document.add(new Field("filename", f.getName(), Field.Store.YES, Field.Index.NO));
            writer.addDocument(document);
        }
    }

    public static void main(String[] args) throws IOException {
        String dir = "d:/tddownload/lucene-3.6.1-src";
        if (args.length > 0) {
            dir = args[0];
        }
        Directory directory = FSDirectory.open(new File("index"));
        Analyzer analyzer = new JavaSrcAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(directory, config);
        buildIndex(dir, writer);
        writer.close();
    }
}
