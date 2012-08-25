package com.codingstory.polaris.web.client;

import com.codingstory.polaris.web.shared.SearchResultDto;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class SearchResultEntryWidget extends Composite {
    interface MyUiBinder extends UiBinder<HTMLPanel, SearchResultEntryWidget> {
    }

    public interface Listener {
        public void onViewSource(String fileName);
    }

    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);

    @UiField
    Anchor fileNameAnchor;
    @UiField
    Element summaryElement;
    private final String fileName;
    private final Listener listener;

    public SearchResultEntryWidget(SearchResultDto.Entry entry, Listener listener) {
        Preconditions.checkNotNull(entry);
        Preconditions.checkNotNull(listener);
        initWidget(UI_BINDER.createAndBindUi(this));
        this.fileName = entry.getFileName();
        this.fileNameAnchor.setText(fileName);
        this.summaryElement.setInnerText(entry.getSummary());
        this.listener = listener;
    }

    @UiHandler("fileNameAnchor")
    void onSearchResultClicked(ClickEvent event) {
        listener.onViewSource(fileName);
    }
}
