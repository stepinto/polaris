package com.codingstory.polaris.web.client;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.List;

public class SearchBox extends Composite implements HasAllKeyHandlers, Focusable, HasText, HasEnabled {

    private static final CodeSearchServiceAsync RPC_SERVICE = GWT.create(CodeSearchService.class);

    private static class QueryCompletionSuggestion implements SuggestOracle.Suggestion {
        private final String s;

        private QueryCompletionSuggestion(String s) {
            this.s = s;
        }

        @Override
        public String getDisplayString() {
            return s;
        }

        @Override
        public String getReplacementString() {
            return s;
        }
    }

    private static class QueryCompletionSuggestOracle extends SuggestOracle {
        @Override
        public void requestSuggestions(final Request req, final Callback callback) {
            RPC_SERVICE.completeQuery(req.getQuery(), req.getLimit(), new AsyncCallback<List<String>>() {
                @Override
                public void onFailure(Throwable caught) {
                }

                @Override
                public void onSuccess(List<String> result) {
                    Response resp = new Response();
                    resp.setSuggestions(Lists.transform(result, new Function<String, Suggestion>() {
                        @Override
                        public Suggestion apply(String s) {
                            return new QueryCompletionSuggestion(s);
                        }
                    }));
                    callback.onSuggestionsReady(req, resp);
                }
            });
        }
    }

    private final SuggestBox searchBox = new SuggestBox(new QueryCompletionSuggestOracle());

    public SearchBox() {
        initWidget(searchBox);
    }

    @Override
    public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
        return searchBox.addKeyDownHandler(handler);
    }

    @Override
    public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
        return searchBox.addKeyPressHandler(handler);
    }

    @Override
    public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
        return searchBox.addKeyUpHandler(handler);
    }

    @Override
    public int getTabIndex() {
        return searchBox.getTabIndex();
    }

    @Override
    public void setAccessKey(char key) {
        searchBox.setAccessKey(key);
    }

    @Override
    public void setFocus(boolean focused) {
        searchBox.setFocus(focused);
    }

    @Override
    public void setTabIndex(int index) {
        searchBox.setTabIndex(index);
    }

    @Override
    public String getText() {
        return searchBox.getText();
    }

    @Override
    public void setText(String text) {
        searchBox.setText(text);
    }

    @Override
    public boolean isEnabled() {
        return searchBox.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        searchBox.setEnabled(enabled);
    }

}
