package com.codingstory.polaris.indexing;

import com.codingstory.polaris.indexing.analysis.JavaSrcAnalyzer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-17
 * Time: 下午7:21
 * To change this template use File | Settings | File Templates.
 */
public class JavaIndexer implements Closeable {

    private static Log LOG = LogFactory.getLog(JavaIndexer.class);

    private final Directory indexDir;
    private final IndexWriterConfig config;
    private final IndexWriter writer;

    public JavaIndexer(File indexDir) throws IOException {
        this.indexDir = FSDirectory.open(indexDir);
        config = new IndexWriterConfig(Version.LUCENE_36, new JavaSrcAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        writer = new IndexWriter(this.indexDir, config);
    }

    public void indexDirectory(File dir) throws IOException {
        for (File file : FileUtils.listFiles(dir, new SuffixFileFilter(".java"), HiddenFileFilter.VISIBLE)) {
            indexFile(file);
        }
    }

    public void indexFile(File file) throws IOException {
        LOG.debug("Indexing " + file);
        String fileContent = FileUtils.readFileToString(file);
        Document document = new Document();
        document.add(new Field("content", fileContent, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        document.add(new Field("filename", file.getName(), Field.Store.YES, Field.Index.NO));
        writer.addDocument(document);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
