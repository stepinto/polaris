package com.codingstory.polaris.web.server;

import com.codingstory.polaris.search.Result;
import com.codingstory.polaris.search.SrcSearcher;
import com.codingstory.polaris.web.client.CodeSearchService;
import com.codingstory.polaris.web.shared.SearchResultDto;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class CodeSearchServiceImpl extends RemoteServiceServlet implements CodeSearchService {

    private static final String CODE = "main() {\n    printf(\"Hello, world!\\n\");\n}";
    private static final Log LOG = LogFactory.getLog(CodeSearchServiceImpl.class);

    @Override
    public List<SearchResultDto> search(String query) {
        try {
            SrcSearcher search = new SrcSearcher("index");
            List<Result> results = search.search(query, 100);
            return ImmutableList.copyOf(Lists.transform(results, new Function<Result, SearchResultDto>() {
                @Override
                public SearchResultDto apply(Result result) {
                    return convertSearchResultToDto(result);
                }
            }));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String readFile(String fileName) {
        return CODE;
    }

    private SearchResultDto convertSearchResultToDto(Result result) {
        SearchResultDto dto = new SearchResultDto();
        dto.setFileName(result.getFilename());
        dto.setSummary(result.getSummary());
        return dto;
    }
}

