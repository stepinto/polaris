package com.codingstory.polaris.web.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.History;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AppEntryPoint implements EntryPoint {
    @Override
    public void onModuleLoad() {
        PageController.handleHistoryToken(History.getToken());
    }
}
