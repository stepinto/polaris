package com.codingstory.polaris.web.client;

import com.codingstory.polaris.web.client.stub.CodeSearchStub;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHyperlink;

import static com.codingstory.polaris.web.client.stub.CodeSearchStub.*;

public class ViewSourcePage extends Composite {
    interface MyUiBinder extends UiBinder<HTMLPanel, ViewSourcePage> {
    }

    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);
    @UiField
    CodeHighlightWidget code;

    public ViewSourcePage(byte[] fileId) {
        Preconditions.checkNotNull(fileId);
        initWidget(UI_BINDER.createAndBindUi(this));
        SourceRequest req = new SourceRequest();
        req.setFileId(fileId);
        CodeSearchStub.source(req, new Callback<SourceResponse, Throwable>() {
            @Override
            public void onFailure(Throwable e) {
                PageController.switchToErrorPage(e);
            }

            @Override
            public void onSuccess(SourceResponse resp) {
                showSourceCode(resp);
            }
        });
    }

    private void showSourceCode(SourceResponse resp) {
        Preconditions.checkNotNull(resp);
        String content = resp.getContent();
        code.setText(content);
        for (Token token : resp.getTokens()) {
            TokenSpan span = token.getSpan();
            if (token.hasTypeDeclaration()) {
                InlineHyperlink link = new InlineHyperlink();
                TypeDeclaration typeDecl = token.getTypeDeclaration();
                link.setTargetHistoryToken("p=search&q=FieldTypeName:" + URL.encode(typeDecl.getName()));
                code.bindTokenWidget(span.getFrom(), link);
            } else if (token.hasTypeUsage()) {
                TypeReference typeRef = token.getTypeUsage().getTypeRefernece();
                InlineHyperlink link = new InlineHyperlink();
                if (typeRef.isResolved()) {
                    String typeName = Iterables.getOnlyElement(typeRef.getCandidates());
                    link.setTargetHistoryToken("p=search&q=TypeName:" + URL.encode(typeName));
                    link.setText(content.substring(span.getFrom(), span.getTo()));
                    code.bindTokenWidget(span.getFrom(), link);
                }
            }
        }
    }
}
