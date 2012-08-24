package com.codingstory.polaris.searchui.client;

import com.google.gwt.user.client.ui.RootPanel;

import java.util.List;

public class PageController {
    public static void switchToErrorPage(Throwable e) {
    }

    public static void switchToSearchResult(List<SearchResultTransfer> results) {
        SearchResultPage searchResultPage = new SearchResultPage();
        searchResultPage.bind(results);
        RootPanel rootPanel = RootPanel.get();
        rootPanel.clear();
        rootPanel.add(searchResultPage);
    }

    public static void switchToViewSource(String fileName) {
        ViewSourcePage viewSourcePage = new ViewSourcePage(fileName);
        RootPanel rootPanel = RootPanel.get();
        rootPanel.clear();
        rootPanel.add(viewSourcePage);
    }
}
