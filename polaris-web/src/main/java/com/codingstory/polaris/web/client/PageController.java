package com.codingstory.polaris.web.client;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Map;

public class PageController {
    static {
        History.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                handleHistoryToken(event.getValue());
            }
        });
    }

    public static void handleHistoryToken(String token) {
        Map<String, String> parameters = buildParameters(token);
        String page = parameters.get("p");
        if (Objects.equal(page, "search")) {
            doSwitchToSearchPage(parameters.get("q"));
        } else if (Objects.equal(page, "source")) {
            doSwitchToSourcePage(HexUtils.stringToHex(parameters.get("file")));
        } else if (Objects.equal(page, "error")) {
            doSwitchToErrorPage(parameters.get("msg"));
        } else {
            doSwitchToHomePage();
        }
    }

    private static Map<String, String> buildParameters(String token) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (String part : Splitter.on("&").omitEmptyStrings().split(token)) {
            int eq = part.indexOf('=');
            if (eq >= part.length()) {
                builder.put(URL.decode(part), "");
            } else {
                String key = URL.decode(part.substring(0, eq));
                String value = URL.decode(part.substring(eq + 1));
                builder.put(key, value);
            }
        }
        return builder.build();
    }

    public static void switchToErrorPage(Throwable e) {
        Preconditions.checkNotNull(e);
        switchToErrorPage(e.toString());
    }

    public static void switchToErrorPage(String msg) {
        Preconditions.checkNotNull(msg);
        History.newItem("p=error&msg=" + URL.encode(msg));
    }

    public static void switchToSearchResult(String query) {
        Preconditions.checkNotNull(query);
        History.newItem("p=search&q=" + URL.encode(query));
    }

    public static void switchToViewSource(byte[] fileId) {
        Preconditions.checkNotNull(fileId);
        History.newItem("p=source&file=" + URL.encode(HexUtils.hexToString(fileId)));
    }

    private static void doSwitchToHomePage() {
        attachWidgetToRootPanel(new HomePage());
    }

    private static void doSwitchToSearchPage(String query) {
        attachWidgetToRootPanel(new SearchResultPage(query));
    }

    private static void doSwitchToSourcePage(byte[] fileId) {
        attachWidgetToRootPanel(new ViewSourcePage(fileId));
    }

    private static void doSwitchToErrorPage(String msg) {
        attachWidgetToRootPanel(ErrorPage.createFromPlainText(msg));
    }


    private static void attachWidgetToRootPanel(Widget widget) {
        RootPanel rootPanel = RootPanel.get();
        rootPanel.clear();
        rootPanel.add(widget);
    }
}
