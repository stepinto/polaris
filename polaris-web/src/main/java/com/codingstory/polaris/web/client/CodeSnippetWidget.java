package com.codingstory.polaris.web.client;

import com.google.common.base.Preconditions;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

/**
 * Shows a piece of code snippet with syntax highlighting.
 */
public class CodeSnippetWidget extends Composite {
    static interface MyUiBinder extends UiBinder<HTMLPanel, CodeSnippetWidget> {
    }

    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);

    @UiField
    PreElement codeSnippetPreElement;

    public CodeSnippetWidget() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    public void setText(String text) {
        Preconditions.checkNotNull(text);
        codeSnippetPreElement.setInnerHTML(NativeHelper.prettyPrint(text));
    }
}
