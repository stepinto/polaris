package com.codingstory.polaris.web.client;

import com.google.common.base.Preconditions;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;

public class ErrorPage extends Composite {
    private ErrorPage(HTML html) {
        initWidget(html);
    }

    public static ErrorPage createFromPlainText(String text) {
        Preconditions.checkNotNull(text);
        HTML html = new HTML(new SafeHtmlBuilder()
                .appendHtmlConstant("<pre>")
                .appendEscaped(text)
                .appendHtmlConstant("</pre>")
                .toSafeHtml());
        return createFromHTML(html);
    }

    public static ErrorPage createFromHTML(HTML html) {
        return new ErrorPage(Preconditions.checkNotNull(html));
    }
}
