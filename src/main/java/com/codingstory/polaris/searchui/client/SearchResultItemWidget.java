package com.codingstory.polaris.searchui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class SearchResultItemWidget extends Composite {
    interface MyUiBinder extends UiBinder<VerticalPanel, SearchResultItemWidget> {
    }
    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);

    @UiField
    Label fileNameLabel;
    @UiField
    Label summaryLabel;

    public SearchResultItemWidget() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    public void bind(SearchResultTransfer result) {
        fileNameLabel.setText(result.getFileName());
        summaryLabel.setText(result.getSummary());
    }
}
