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
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    public String readFile(String fileId) {
        try {
            SrcSearcher searcher = new SrcSearcher("index");
            return searcher.getContent(Hex.decodeHex(fileId.toCharArray()));
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> completeQuery(String query, int limit) {
        try {
            SrcSearcher searcher = new SrcSearcher("index");
            return searcher.completeQuery(query, limit);
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            throw new RuntimeException(e);
        }
    }

    private SearchResultDto.Entry convertSearchResultToDtoEntry(Result result) {
        SearchResultDto.Entry e = new SearchResultDto.Entry();
        e.setProjectName(result.getProjectName());
        e.setFileName(result.getFilename());
        e.setFileId(Hex.encodeHexString(result.getFileId()));
        e.setSummary(result.getSummary());
        e.setExplanation(result.getExplanation().toHtml());
        e.setScore(result.getExplanation().getValue());
        e.setKind(result.getKind());
        return e;
    }
}

