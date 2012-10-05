package com.codingstory.polaris.web.client;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

public class MainFrame extends Composite {
    interface MyUiBinder extends UiBinder<HTMLPanel, MainFrame> {
    }

    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);
    private static final RegExp FILE_ID_OFFSET_PATTERN = RegExp.compile("([0-9a-f]{40}):([0-9]+)");

    @UiField
    SearchBox searchBox;
    @UiField
    SimplePanel mainPanel;

    public MainFrame() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    public void executeQuery(String query) {
        searchBox.setText(query);
        searchBox.setEnabled(false);
        showLoadingScreen();
        MatchResult m = FILE_ID_OFFSET_PATTERN.exec(query);
        if (m != null) {
            String fileId = m.getGroup(1);
            int offset = Integer.parseInt(m.getGroup(2));
            ViewSourcePage.create(fileId, offset, new Callback<ViewSourcePage, Throwable>() {
                @Override
                public void onFailure(Throwable reason) {
                    handleError(reason);
                }

                @Override
                public void onSuccess(ViewSourcePage result) {
                    mainPanel.setWidget(result);
                    searchBox.setEnabled(true);
                }
            });
        } else {
            SearchResultPage.create(query, new Callback<SearchResultPage, Throwable>() {
                @Override
                public void onFailure(Throwable reason) {
                    handleError(reason);
                }

                @Override
                public void onSuccess(SearchResultPage result) {
                    mainPanel.setWidget(result);
                    searchBox.setEnabled(true);
                }
            });
        }
    }

    private void handleError(Throwable reason) {
        PageController.switchToErrorPage(reason);
    }

    private void showLoadingScreen() {
        Label label = new Label();
        label.setText("Loading...");
        mainPanel.setWidget(label);
    }
}
