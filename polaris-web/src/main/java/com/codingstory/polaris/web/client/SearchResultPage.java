package com.codingstory.polaris.web.client;

import com.codingstory.polaris.web.shared.SearchResultDto;
import com.google.common.base.Stopwatch;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.logging.Logger;

public class SearchResultPage extends Composite {
    interface MyUiBinder extends UiBinder<HTMLPanel, SearchResultPage> {
    }
    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);
    private static final CodeSearchServiceAsync RPC_SERVICE = GWT.create(CodeSearchService.class);
    private static final Logger LOGGER = Logger.getLogger(SearchResultPage.class.getName());

    @UiField
    TextBox searchBox;
    @UiField
    Button searchButton;
    @UiField
    FlowPanel searchResultListPanel;
    @UiField
    Label latencyLabel;

    public SearchResultPage(String query) {
        initWidget(UI_BINDER.createAndBindUi(this));
        executeSearch(query);
    }

    @UiHandler("searchButton")
    void onSearchButton(ClickEvent event) {
        String query = searchBox.getText();
        PageController.switchToSearchResult(query);
    }

    private void executeSearch(String query) {
        final Stopwatch stopwatch = new Stopwatch().start();
        searchBox.setText(query);
        RPC_SERVICE.search(query, new AsyncCallback<SearchResultDto>() {
            @Override
            public void onFailure(Throwable caught) {
                LOGGER.warning(caught.toString());
            }

            @Override
            public void onSuccess(SearchResultDto searchResults) {
                searchResultListPanel.clear();
                for (SearchResultDto.Entry result : searchResults.getEntries()) {
                    SearchResultEntryWidget entryWidget = new SearchResultEntryWidget(
                            result, new SearchResultEntryWidget.Listener() {
                        @Override
                        public void onViewSource(String fileName) {
                            PageController.switchToViewSource(fileName);
                        }
                    });
                    searchResultListPanel.add(entryWidget);
                }
                latencyLabel.setText("Latency: " + String.valueOf(searchResults.getLatency()) + " ms");
            }
        });
    }
}
