package com.codingstory.polaris.web.client;

import com.codingstory.polaris.web.shared.SearchResultDto;
import com.google.common.base.Stopwatch;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
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
    SearchBox searchBox;
    @UiField
    Button searchButton;
    @UiField
    FlowPanel searchResultListPanel;
    @UiField
    Label latencyLabel;

    public SearchResultPage(String query) {
        initWidget(UI_BINDER.createAndBindUi(this));
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                searchBox.setFocus(true);
            }
        });
        enableSearchPanel(false);
        executeSearch(query);
    }

    @UiHandler("searchButton")
    void onSearchButton(ClickEvent event) {
        String query = searchBox.getText();
        PageController.switchToSearchResult(query);
    }

    @UiHandler("searchBox")
    void onSearchBoxKeyDown(KeyDownEvent event) {
        if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
            NativeHelper.click(searchButton.getElement());
        }
    }

    private void executeSearch(String query) {
        final Stopwatch stopwatch = new Stopwatch().start();
        searchBox.setText(query);
        RPC_SERVICE.search(query, new AsyncCallback<SearchResultDto>() {
            @Override
            public void onFailure(Throwable caught) {
                enableSearchPanel(true);
                LOGGER.warning(caught.toString());
            }

            @Override
            public void onSuccess(SearchResultDto searchResults) {
                searchResultListPanel.clear();
                for (SearchResultDto.Entry result : searchResults.getEntries()) {
                    String href = "p=source&file=" + result.getFileName();
                    SearchResultEntryWidget entryWidget = new SearchResultEntryWidget(result, href);
                    searchResultListPanel.add(entryWidget);
                }
                latencyLabel.setText("Latency: " + String.valueOf(searchResults.getLatency()) + " ms");
                enableSearchPanel(true);
            }
        });
    }

    private void enableSearchPanel(boolean enabled) {
        searchBox.setEnabled(enabled);
        searchButton.setEnabled(enabled);
    }
}
