package com.codingstory.polaris.web.client;

import com.google.gwt.dom.client.Element;

public final class NativeEventHelper {
    public static native void click(Element element) /*-{
        element.click();
    }-*/;
}
