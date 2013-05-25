package com.codingstory.polaris.usagedb;

import com.codingstory.polaris.SnappyUtils;
import com.codingstory.polaris.parser.ParserProtos.Usage;
import com.codingstory.polaris.parser.TypeUtils;
import com.codingstory.polaris.usagedb.UsageDbProtos.UsageData;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
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
    public List<Usage> query(Usage.Kind kind, long id) throws IOException {
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(new Term(UsageDbIndexedField.KIND, String.valueOf(kind.getNumber()))),
                BooleanClause.Occur.MUST);
        query.add(new TermQuery(new Term(UsageDbIndexedField.ID, String.valueOf(id))), BooleanClause.Occur.MUST);
        TopDocs result = searcher.search(query, Integer.MAX_VALUE);
        List<Usage> usages = Lists.newArrayList();
        for (ScoreDoc scoreDoc : result.scoreDocs) {
            usages.add(retrieveDocument(scoreDoc.doc));
        }
        sortUsagesByJumpTarget(usages);
        return usages;
    }

    @Override
    public List<Usage> findUsagesInFile(long fileId) throws IOException {
        TermQuery query = new TermQuery(new Term(UsageDbIndexedField.FILE_ID, String.valueOf(fileId)));
        TopDocs result = searcher.search(query, Integer.MAX_VALUE);
        List<Usage> usages = Lists.newArrayListWithCapacity(result.scoreDocs.length);
        for (ScoreDoc scoreDoc : result.scoreDocs) {
            usages.add(retrieveDocument(scoreDoc.doc));
        }
        sortUsagesByJumpTarget(usages);
        return usages;
    }

    private void sortUsagesByJumpTarget(List<Usage> usages) {
        Collections.sort(usages, new Comparator<Usage>() {
            @Override
            public int compare(Usage left, Usage right) {
                return TypeUtils.JUMP_TARGET_COMPARATOR.compare(left.getJumpTarget(), right.getJumpTarget());
            }
        });
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private Usage retrieveDocument(int docId) throws IOException {
        Document document = reader.document(docId);
        BytesRef bytesRef = document.getBinaryValue(UsageDbIndexedField.USAGE_DATA);
        UsageData usageData = UsageData.parseFrom(
                SnappyUtils.uncompress(bytesRef.bytes, bytesRef.offset, bytesRef.length));
        return usageData.getUsage();
    }
}
