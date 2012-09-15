package com.codingstory.polaris.web.server;

import com.codingstory.polaris.search.Result;
import com.codingstory.polaris.search.SrcSearcher;
import com.codingstory.polaris.web.client.CodeSearchService;
import com.codingstory.polaris.web.shared.SearchResultDto;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableAsList_CustomFieldSerializer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class GWTCodeSearchServiceImpl extends RemoteServiceServlet implements CodeSearchService {
    private static final Log LOG = LogFactory.getLog(GWTCodeSearchServiceImpl.class);

    @Override
    public List<String> completeQuery(String query, int limit) {
        return ImmutableList.of();
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

