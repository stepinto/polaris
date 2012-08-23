package com.codingstory.polaris.searchui.client;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

import java.util.List;

public class SearchResultPage extends Composite {
    interface MyUiBinder extends UiBinder<HTMLPanel, SearchResultPage> {
    }
    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);

    @UiField
    Element searchResultListElement;

    public SearchResultPage() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    public void bind(List<SearchResultTransfer> results) {
        for (SearchResultTransfer result : results) {
            SearchResultItemWidget itemWidget = new SearchResultItemWidget();
            itemWidget.bind(result);
            searchResultListElement.appendChild(itemWidget.getElement());
        }
    }
}
