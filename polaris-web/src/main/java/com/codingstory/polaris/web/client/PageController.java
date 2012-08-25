package com.codingstory.polaris.web.client;

import com.google.gwt.user.client.ui.RootPanel;

public class PageController {
    public static void switchToErrorPage(Throwable e) {
    }

    public static void switchToSearchResult(String query) {
        SearchResultPage searchResultPage = new SearchResultPage(query);
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
