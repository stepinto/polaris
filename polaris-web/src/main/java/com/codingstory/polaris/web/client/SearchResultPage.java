package com.codingstory.polaris.web.client;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;

import java.util.List;

public class SearchResultPage extends Composite {
    interface MyUiBinder extends UiBinder<HTMLPanel, SearchResultPage> {
    }
    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);

    @UiField
    TextBox searchBox;
    @UiField
    Button searchButton;
    @UiField
    FlowPanel searchResultListPanel;

    public SearchResultPage() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    public void bind(List<SearchResultTransfer> results) {
        for (SearchResultTransfer result : results) {
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
}
