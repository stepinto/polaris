package com.codingstory.polaris.usagedb;

import com.codingstory.polaris.SnappyUtils;
import com.codingstory.polaris.parser.ParserProtos.Usage;
import com.codingstory.polaris.usagedb.UsageDbProtos.UsageData;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class UsageDbImpl implements UsageDb {
    private final IndexReader reader;
    private final IndexSearcher searcher;

    public UsageDbImpl(File path) throws IOException {
        Preconditions.checkNotNull(path);
        reader = IndexReader.open(FSDirectory.open(path));
        searcher = new IndexSearcher(reader);
    }

    @Override
    public List<Usage> query(long typeId) throws IOException {
        TermQuery query = new TermQuery(new Term(UsageDbIndexedField.TYPE_ID_RAW, String.valueOf(typeId)));
        TopDocs result = searcher.search(query, Integer.MAX_VALUE);
        List<Usage> typeUsageResult = Lists.newArrayList();
        for (ScoreDoc scoreDoc : result.scoreDocs) {
            typeUsageResult.add(retrieveDocument(scoreDoc.doc));
        }
        return typeUsageResult;
    }

    @Override
    public void close() throws IOException {
        reader.close();
        searcher.close();
    }

    private Usage retrieveDocument(int docId) throws IOException {
        Document document = reader.document(docId);
        byte[] binaryData = document.getBinaryValue(UsageDbIndexedField.USAGE_DATA);
        UsageData usageData = UsageData.parseFrom(SnappyUtils.uncompress(binaryData));
        return usageData.getUsage();
    }
}
