package com.codingstory.polaris.web.server;

import com.codingstory.polaris.search.Result;
import com.codingstory.polaris.search.SrcSearcher;
import com.codingstory.polaris.web.client.CodeSearchService;
import com.codingstory.polaris.web.shared.SearchResultDto;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.util.List;

public class CodeSearchServiceImpl extends RemoteServiceServlet implements CodeSearchService {
    private static final Log LOG = LogFactory.getLog(CodeSearchServiceImpl.class);

    @Override
    public SearchResultDto search(String query) {
        try {
            Stopwatch stopwatch = new Stopwatch().start();
            SrcSearcher search = new SrcSearcher("index");
            List<Result> results = search.search(query, 100);
            SearchResultDto searchResultDto = new SearchResultDto();
            searchResultDto.setEntries(ImmutableList.copyOf(
                    Lists.transform(results, new Function<Result, SearchResultDto.Entry>() {
                        @Override
                        public SearchResultDto.Entry apply(Result result) {
                            return convertSearchResultToDtoEntry(result);
                        }
                    })));
            searchResultDto.setLatency(stopwatch.elapsedMillis());
            return searchResultDto;
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String readFile(String fileName) {
        // TODO: Use our searcher interface
        IndexReader reader = null;
        try {
            reader = IndexReader.open(FSDirectory.open(new File("index")));
            Query query = new TermQuery(new Term("filename", fileName));
            int docid = new IndexSearcher(reader).search(query, 1).scoreDocs[0].doc;
            return reader.document(docid).get("content");
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private SearchResultDto.Entry convertSearchResultToDtoEntry(Result result) {
        SearchResultDto.Entry e = new SearchResultDto.Entry();
        e.setFileName(result.getFilename());
        e.setSummary(result.getSummary());
        e.setExplanation(result.getExplanation().toHtml());
        e.setScore(result.getExplanation().getValue());
        e.setKind(result.getKind());
        return e;
    }
}

