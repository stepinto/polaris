package com.codingstory.polaris.searchui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class SearchResultItemWidget extends Composite {
    interface MyUiBinder extends UiBinder<HTMLPanel, SearchResultItemWidget> {
    }

    public interface Listener {
        public void onViewSource(String fileName);
    }

    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);

    @UiField
    Anchor fileNameAnchor;
    @UiField
    Element summaryElement;
    private String fileName;
    private Listener listener;

    public SearchResultItemWidget() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    public void bind(SearchResultTransfer result) {
        this.fileName = result.getFileName();
        fileNameAnchor.setText(result.getFileName());
        summaryElement.setInnerText(result.getSummary());
        fileNameAnchor.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @UiHandler("fileNameAnchor")
    void onSearchResultClicked(ClickEvent event) {
        if (listener != null) {
            listener.onViewSource(fileName);
        }
    }
}
