package com.codingstory.polaris.web.client;

import com.codingstory.polaris.web.shared.SearchResultDto;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Hyperlink;

public class SearchResultEntryWidget extends Composite {
    static interface MyUiBinder extends UiBinder<HTMLPanel, SearchResultEntryWidget> {
    }
    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);

    @UiField
    Hyperlink fileNameAnchor;
    @UiField
    CodeSnippetWidget summaryCodeSnippet;

    public SearchResultEntryWidget(SearchResultDto.Entry entry, String href) {
        Preconditions.checkNotNull(entry);
        Preconditions.checkNotNull(href);
        initWidget(UI_BINDER.createAndBindUi(this));
        this.fileNameAnchor.setText(entry.getFileName());
        this.fileNameAnchor.setTargetHistoryToken(href);
        this.summaryCodeSnippet.setText(entry.getSummary());
    }
}
