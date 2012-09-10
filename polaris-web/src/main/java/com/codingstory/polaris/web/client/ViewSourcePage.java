package com.codingstory.polaris.web.client;

import com.google.common.base.Preconditions;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHyperlink;

public class ViewSourcePage extends Composite {
    interface MyUiBinder extends UiBinder<HTMLPanel, ViewSourcePage> {
    }

    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);
    @UiField
    CodeHighlightWidget code;

    public ViewSourcePage(String fileId) {
        initWidget(UI_BINDER.createAndBindUi(this));
        String url = "/source?f=" + URL.encode(fileId);
        RequestBuilder reqBuilder = new RequestBuilder(RequestBuilder.GET, url);
        try {
            reqBuilder.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request req, Response resp) {
                    if (resp.getStatusCode() != Response.SC_OK) {
                        // TODO: report error
                        return;
                    }
                    showSourceCode(JSONParser.parseStrict(resp.getText()).isObject());
                }

                @Override
                public void onError(Request req, Throwable e) {
                    // TODO: report error
                }
            });
        } catch (RequestException e) {
            // TODO: report error
        }
    }

    private void showSourceCode(JSONObject resp) {
        Preconditions.checkNotNull(resp);
        String content = resp.get("content").isString().stringValue();
        code.setText(content);
        JSONArray tokens = resp.get("tokens").isArray();
        for (int i = 0; i < tokens.size(); i++) {
            JSONObject token = tokens.get(i).isObject();
            int spanFrom = (int) token.get("span").isObject().get("from").isNumber().doubleValue();
            int spanTo = (int) token.get("span").isObject().get("to").isNumber().doubleValue();
            if (token.containsKey("typeDeclaration")) {
                JSONObject typeDecl = token.get("typeDeclaration").isObject();
                InlineHyperlink link = new InlineHyperlink();
                String typeName = typeDecl.get("name").isString().stringValue();
                link.setTargetHistoryToken("p=search&q=FieldTypeName:" + URL.encode(typeName));
                code.bindTokenWidget(spanFrom, link);
            } else if (token.containsKey("typeUsage")) {
                JSONObject typeUsage = token.get("typeUsage").isObject();
                InlineHyperlink link = new InlineHyperlink();
                JSONObject typeRef = typeUsage.get("typeReference").isObject();
                int resolved = (int) typeRef.get("resolved").isNumber().doubleValue();
                if (resolved == 1) {
                    String typeName = typeRef.get("candidates").isArray().get(0).isString().stringValue();
                    link.setTargetHistoryToken("p=search&q=TypeName:" + URL.encode(typeName));
                    link.setText(content.substring(spanFrom, spanTo));
                    code.bindTokenWidget(spanFrom, link);
                }
            }
        }
    }
}
