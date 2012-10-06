package com.codingstory.polaris.web.client;

import com.codingstory.polaris.web.client.stub.CodeSearchStub;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;

import static com.codingstory.polaris.web.client.stub.CodeSearchStub.SearchRequest;
import static com.codingstory.polaris.web.client.stub.CodeSearchStub.SearchResponse;
import static com.codingstory.polaris.web.client.stub.CodeSearchStub.StatusCode;

public class SearchResultPage extends Composite {
    interface MyUiBinder extends UiBinder<HTMLPanel, SearchResultPage> {
    }
    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);

    @UiField
    FlowPanel searchResultListPanel;

    private SearchResultPage() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    public static void create(String query, final Callback<SearchResultPage, Throwable> callback) {
        SearchRequest req = new SearchRequest();
        req.setQuery(query);
        CodeSearchStub.search(req, new Callback<SearchResponse, Throwable>() {
            @Override
            public void onFailure(Throwable reason) {
                callback.onFailure(reason);
            }

            @Override
            public void onSuccess(SearchResponse resp) {
                if (resp.getStatus() != StatusCode.OK) {
                    callback.onFailure(new Exception("Bad status: " + resp.getStatus()));
                    return;
                }
                callback.onSuccess(createFromSearchResponse(resp));
            }
        });
    }

    private static SearchResultPage createFromSearchResponse(SearchResponse resp) {
        SearchResultPage page = new SearchResultPage();
        for (SearchResponse.Entry result : resp.getEntries()) {
            String href = "p=main&q=" + URL.encode(result.getFileId()) + ":"+ result.getOffset();
            SearchResultEntryWidget entryWidget = new SearchResultEntryWidget(result, href);
            page.searchResultListPanel.add(entryWidget);
        }
        return page;
    }
}
