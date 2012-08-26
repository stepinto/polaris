package com.codingstory.polaris.web.client;

import com.codingstory.polaris.parser.Token;
import com.codingstory.polaris.web.shared.SearchResultDto;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;

public class SearchResultEntryWidget extends Composite {
    static interface MyUiBinder extends UiBinder<HTMLPanel, SearchResultEntryWidget> {
    }
    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);

    @UiField
    Hyperlink fileNameAnchor;
    @UiField
    Label kindLabel;
    @UiField
    Label rankingScoreLabel;
    @UiField
    DivElement rankingExplanationDivElement;
    @UiField
    CodeSnippetWidget summaryCodeSnippet;

    public SearchResultEntryWidget(SearchResultDto.Entry entry, String href) {
        Preconditions.checkNotNull(entry);
        Preconditions.checkNotNull(href);
        initWidget(UI_BINDER.createAndBindUi(this));
        this.fileNameAnchor.setText(entry.getFileName());
        this.fileNameAnchor.setTargetHistoryToken(href);
        this.summaryCodeSnippet.setText(entry.getSummary());
        if (entry.getKind() == Token.Kind.METHOD_DECLARATION) {
            kindLabel.setText("[class]");
        } else if (entry.getKind() == Token.Kind.METHOD_DECLARATION) {
            kindLabel.setText("[method]");
        } else {
            kindLabel.setVisible(false);
        }
        rankingScoreLabel.setText("Score: " + entry.getScore());
        rankingExplanationDivElement.setInnerHTML(entry.getExplanation());
    }
}
