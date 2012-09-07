package com.codingstory.polaris.web.client;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

import java.util.logging.Logger;

public class ViewSourcePage extends Composite {
    interface MyUiBinder extends UiBinder<HTMLPanel, ViewSourcePage> {
    }

    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);
    private static final CodeSearchServiceAsync RPC_SERVICE = GWT.create(CodeSearchService.class);
    private static final Logger LOGGER = Logger.getLogger("");
    @UiField
    CodeHighlightWidget code;

    public ViewSourcePage(String fileId) {
        initWidget(UI_BINDER.createAndBindUi(this));
        RPC_SERVICE.readFile(fileId, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                LOGGER.warning(caught.toString());
            }

            @Override
            public void onSuccess(String result) {
                code.setText(result);
            }
        });
    }
}
