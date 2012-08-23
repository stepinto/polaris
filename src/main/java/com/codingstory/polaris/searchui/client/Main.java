package com.codingstory.polaris.searchui.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Main implements EntryPoint {
    @Override
    public void onModuleLoad() {
        HomePage homePage = new HomePage();
        RootPanel.get().add(homePage);
    }
}
