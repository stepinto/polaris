package com.codingstory.polaris.searchui.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.List;

@RemoteServiceRelativePath("search")
public interface CodeSearchService extends RemoteService {
    public List<SearchResultTransfer> search(String query);
    public String readFile(String fileName);
}
