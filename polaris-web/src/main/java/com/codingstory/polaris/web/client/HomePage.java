package com.codingstory.polaris.web.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.List;

public class HomePage extends Composite {
    interface MyUiBinder extends UiBinder<HTMLPanel, HomePage> {
    }
    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);
    private static final CodeSearchServiceAsync RPC_SERVICE = GWT.create(CodeSearchService.class);

    @UiField
    TextBox searchBox;
    @UiField
    Button searchButton;

    public HomePage() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    @UiHandler("searchButton")
    void onSearchButton(ClickEvent event) {
        String query = searchBox.getText();
        RPC_SERVICE.search(query, new AsyncCallback<List<SearchResultTransfer>>() {
            @Override
            public void onFailure(Throwable caught) {
                PageController.switchToErrorPage(caught);
            }

            @Override
            public void onSuccess(List<SearchResultTransfer> result) {
                PageController.switchToSearchResult(result);
            }
        });
    }
}
