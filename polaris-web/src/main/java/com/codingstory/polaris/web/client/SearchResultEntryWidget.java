package com.codingstory.polaris.web.client;

import com.codingstory.polaris.parser.Token;
import com.codingstory.polaris.web.shared.SearchResultDto;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;

import static com.codingstory.polaris.web.client.stub.CodeSearchStub.SearchResponse;

import java.util.Map;

public class SearchResultEntryWidget extends Composite {
    static interface MyUiBinder extends UiBinder<HTMLPanel, SearchResultEntryWidget> {
    }
    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);
    private static final Map<Token.Kind, String> TOKEN_KIND_LABELS = ImmutableMap.<Token.Kind, String>builder()
            .put(Token.Kind.CLASS_DECLARATION, "[class]")
            .put(Token.Kind.INTERFACE_DECLARATION, "[interface]")
            .put(Token.Kind.ENUM_DECLARATION, "[enum]")
            .put(Token.Kind.ANNOTATION_DECLARATION, "[annotation]")
            .put(Token.Kind.FIELD_DECLARATION, "[field]")
            .put(Token.Kind.METHOD_DECLARATION, "[method]")
            .build();

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

    public SearchResultEntryWidget(SearchResponse.Entry entry, String href) {
        Preconditions.checkNotNull(entry);
        Preconditions.checkNotNull(href);
        initWidget(UI_BINDER.createAndBindUi(this));
        this.fileNameAnchor.setText(entry.getProjectName() + entry.getFileName());
        this.fileNameAnchor.setTargetHistoryToken(href);
        this.summaryCodeSnippet.setText(entry.getSummary());
        String label = TOKEN_KIND_LABELS.get(entry.getKind());
        if (label == null) {
            kindLabel.setVisible(false);
        } else {
            kindLabel.setText(label);
        }
        rankingScoreLabel.setText("Score: " + entry.getScore());
        rankingExplanationDivElement.setInnerHTML(entry.getExplanation());
    }
}
