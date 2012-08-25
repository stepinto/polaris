package com.codingstory.polaris.web.client;

import com.codingstory.polaris.web.shared.SearchResultDto;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.List;
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

    public SearchResultPage(String query) {
        initWidget(UI_BINDER.createAndBindUi(this));
        executeSearch(query);
    }

    @UiHandler("searchButton")
    void onSearchButton(ClickEvent event) {
        String query = searchBox.getText();
        executeSearch(query);
    }

    private void executeSearch(String query) {
        searchBox.setText(query);
        RPC_SERVICE.search(query, new AsyncCallback<List<SearchResultDto>>() {
            @Override
            public void onFailure(Throwable caught) {
                LOGGER.warning(caught.toString());
            }

            @Override
            public void onSuccess(List<SearchResultDto> searchResults) {
                searchResultListPanel.clear();
                for (SearchResultDto result : searchResults) {
                    SearchResultItemWidget itemWidget = new SearchResultItemWidget();
                    itemWidget.bind(result);
                    itemWidget.setListener(new SearchResultItemWidget.Listener() {
                        @Override
                        public void onViewSource(String fileName) {
                            PageController.switchToViewSource(fileName);
                        }
                    });
                    searchResultListPanel.add(itemWidget);
                }
            }
        });
    }
}
