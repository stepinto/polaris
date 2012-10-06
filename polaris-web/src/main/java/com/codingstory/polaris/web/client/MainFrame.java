package com.codingstory.polaris.web.client;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
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
    Button searchButton;
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

    @UiHandler("searchBox")
    void onSearchBoxKeyDown(KeyDownEvent event) {
        if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
            // Defer firing the event to make the query completion happens first.
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    NativeHelper.click(searchButton.getElement());
                }
            });
        }
    }

    @UiHandler("searchButton")
    void onSearchButtonClick(ClickEvent event) {
        executeQuery(searchBox.getText());
    }

}
