package com.codingstory.polaris.web.client;

import com.codingstory.polaris.web.shared.SearchResultDto;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.List;

public class PageController {
    public static void switchToErrorPage(Throwable e) {
    }

    public static void switchToSearchResult(List<SearchResultDto> results) {
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
