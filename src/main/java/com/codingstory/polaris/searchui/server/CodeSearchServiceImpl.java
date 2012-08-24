package com.codingstory.polaris.searchui.server;

import com.codingstory.polaris.searchui.client.CodeSearchService;
import com.codingstory.polaris.searchui.client.SearchResultTransfer;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.util.ArrayList;
import java.util.List;

public class CodeSearchServiceImpl extends RemoteServiceServlet implements CodeSearchService {

    private final static String CODE = "main() {\n    printf(\"Hello, world!\\n\");\n}";

    @Override
    public List<SearchResultTransfer> search(String query) {
        SearchResultTransfer result = new SearchResultTransfer();
        result.setFileName("filename");
        result.setSummary(CODE);
        List<SearchResultTransfer> results = new ArrayList<SearchResultTransfer>();
        for (int i = 0; i < 10; i++) {
            results.add(result);
        }
        return results;
    }

    @Override
    public String readFile(String fileName) {
        return CODE;
    }
}
