package com.codingstory.polaris.searchui.server;

import com.codingstory.polaris.searchui.client.CodeSearchService;
import com.codingstory.polaris.searchui.client.SearchResultTransfer;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.util.ArrayList;
import java.util.List;

public class CodeSearchServiceImpl extends RemoteServiceServlet implements CodeSearchService {
    @Override
    public List<SearchResultTransfer> search(String query) {
        SearchResultTransfer result = new SearchResultTransfer();
        result.setFileName("filename");
        result.setSummary("summary");
        List<SearchResultTransfer> results = new ArrayList<SearchResultTransfer>();
        for (int i = 0; i < 10; i++) {
            results.add(result);
        }
        return results;
    }
}
