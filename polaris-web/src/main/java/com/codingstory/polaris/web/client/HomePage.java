package com.codingstory.polaris.web.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class HomePage extends Composite {
    interface MyUiBinder extends UiBinder<HTMLPanel, HomePage> {
    }
    private static final MyUiBinder UI_BINDER = GWT.create(MyUiBinder.class);

    @UiField
    SearchBox searchBox;
    @UiField
    Button searchButton;

    public HomePage() {
        initWidget(UI_BINDER.createAndBindUi(this));
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                searchBox.setFocus(true);
            }
        });
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
    void onSearchButton(ClickEvent event) {
        String query = searchBox.getText();
        PageController.switchToSearchResult(query);
    }
}
