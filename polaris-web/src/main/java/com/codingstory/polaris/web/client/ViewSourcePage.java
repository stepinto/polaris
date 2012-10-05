package com.codingstory.polaris.web.client;

import com.codingstory.polaris.web.client.stub.CodeSearchStub;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.http.client.URL;
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

    private ViewSourcePage() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    public static void create(String fileId, final int offset, final Callback<ViewSourcePage, Throwable> callback) {
        Preconditions.checkNotNull(fileId);
        Preconditions.checkArgument(fileId.length() == 40, "bad fileId: " + fileId);
        Preconditions.checkArgument(offset >= 0, "bad offset: " + offset);

        SourceRequest req = new SourceRequest();
        req.setFileId(fileId);
        CodeSearchStub.source(req, new Callback<SourceResponse, Throwable>() {
            @Override
            public void onFailure(Throwable e) {
                callback.onFailure(e);
            }

            @Override
            public void onSuccess(SourceResponse resp) {
                if (resp.getStatus() != StatusCode.OK) {
                    callback.onFailure(new Exception("Bad status: " + resp.getStatus()));
                    return;
                }
                callback.onSuccess(createFromSourceCode(resp, offset));
            }
        });
    }

    private static ViewSourcePage createFromSourceCode(SourceResponse resp, int offset) {
        ViewSourcePage page = new ViewSourcePage();
        String content = resp.getContent();
        page.code.setText(content);
        for (Token token : resp.getTokens()) {
            TokenSpan span = token.getSpan();
            if (token.hasTypeDeclaration()) {
                InlineHyperlink link = new InlineHyperlink();
                TypeDeclaration typeDecl = token.getTypeDeclaration();
                link.setTargetHistoryToken("p=main&q=FieldTypeName:" + URL.encode(typeDecl.getName()));
                page.code.bindTokenWidget(span.getFrom(), link);
            } else if (token.hasTypeUsage()) {
                TypeReference typeRef = token.getTypeUsage().getTypeRefernece();
                InlineHyperlink link = new InlineHyperlink();
                if (typeRef.isResolved()) {
                    String typeName = Iterables.getOnlyElement(typeRef.getCandidates());
                    link.setTargetHistoryToken("p=main&q=TypeName:" + URL.encode(typeName));
                    link.setText(content.substring(span.getFrom(), span.getTo()));
                    page.code.bindTokenWidget(span.getFrom(), link);
                }
            }
        }
        page.code.scrollToOffset(offset);
        return page;
    }
}
