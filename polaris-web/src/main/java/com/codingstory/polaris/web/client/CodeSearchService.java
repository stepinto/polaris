package com.codingstory.polaris.web.client;

import com.codingstory.polaris.web.shared.SearchResultDto;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.List;

@RemoteServiceRelativePath("search")
public interface CodeSearchService extends RemoteService {
    public SearchResultDto search(String query);
    public String readFile(String fileId);
    public List<String> completeQuery(String query, int limit);
}
