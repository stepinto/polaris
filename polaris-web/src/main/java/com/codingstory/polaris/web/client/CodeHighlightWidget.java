package com.codingstory.polaris.web.client;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.*;

import java.util.List;
import java.util.Map;

public class CodeHighlightWidget extends Composite implements HasText {
    // Since "int" is a prefix of "interface", we have to check "interface" first.
    private static final ImmutableList<String> JAVA_RESERVED_WORDS = ImmutableList.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "interface", "int", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while");

    private String text = "";
    private HTMLPanel htmlPanel = new HTMLPanel("");
    private Map<Long, String> tokenElementIds = Maps.newHashMap();

    public CodeHighlightWidget() {
        initWidget(htmlPanel);
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = Preconditions.checkNotNull(text);
        render();
    }

    public void bindTokenWidget(long offset, Widget widget) {
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkNotNull(widget);
        String id = tokenElementIds.get(offset);
        if (id == null) {
            return;
        }
        htmlPanel.addAndReplaceElement(widget, id);
    }

    private void render() {
        List<SimpleLexer.Rule> rules = Lists.newArrayList();
        final SafeHtmlBuilder builder = new SafeHtmlBuilder();

        rules.add(new SimpleLexer.Rule() { // block comments
            @Override
            public int execute(String code, int offset) {
                if (match(code, offset, "/*")) {
                    StringBuilder comment = new StringBuilder("/*");
                    int n = 2;
                    while (offset + n < code.length() && !match(code, offset + n - 2, "*/")) {
                        comment.append(code.charAt(offset + n));
                        n++;
                    }
                    renderComment(comment.toString(), builder);
                    return n;
                }
                return 0;
            }
        });
        rules.add(new SimpleLexer.Rule() { // single-line comments
            @Override
            public int execute(String code, int offset) {
                if (match(code, offset, "//")) {
                    StringBuilder comment = new StringBuilder("//");
                    int n = 2;
                    while (offset + n < code.length() && code.charAt(offset + n - 1) != '\n') {
                        comment.append(code.charAt(offset + n));
                        n++;
                    }
                    renderComment(comment.toString(), builder);
                    return n;
                }
                return 0;
            }
        });
        rules.add(new SimpleLexer.Rule() { // reservered words
            @Override
            public int execute(String code, int offset) {
                for (String reserved : JAVA_RESERVED_WORDS) {
                    // This rule is not precise, but it can correctly handle most cases.
                    int len = reserved.length();
                    if (match(code, offset, reserved)
                            && (offset + len >= code.length() || !isIdentifierChar(code.charAt(offset + len)))) {
                        renderReservedWord(reserved, offset, builder);
                        return reserved.length();
                    }
                }
                return 0;
            }
        });
        rules.add(new SimpleLexer.Rule() { // identifiers
            @Override
            public int execute(String code, int offset) {
                StringBuilder id = new StringBuilder();
                int n = 0;
                while (offset + n < code.length() && isIdentifierChar(code.charAt(offset + n))) {
                    id.append(code.charAt(offset + n));
                    n++;
                }
                if (n > 0) {
                    renderIdentifier(id.toString(), offset, builder);
                }
                return n;
            }
        });
        rules.add(new SimpleLexer.Rule() { // string literals
            @Override
            public int execute(String code, int offset) {
                if (code.charAt(offset) != '\"') {
                    return 0;
                }
                StringBuilder literal = new StringBuilder();
                int n = 1;
                literal.append('\"');
                // TODO: handle escaped characters
                while (offset + n < code.length() && code.charAt(offset + n) != '\"') {
                    literal.append(code.charAt(offset + n));
                    n++;
                }
                if (offset + n < code.length()) {
                    literal.append('\"');
                    n++;
                }
                renderStringLiteral(literal.toString(), builder);
                return n;
            }
        });
        rules.add(new SimpleLexer.Rule() { // anything else
            @Override
            public int execute(String code, int offset) {
                renderUnknown(code.charAt(offset), builder);
                return 1;
            }
        });

        SimpleLexer lexer = new SimpleLexer(rules);
        builder.appendHtmlConstant("<pre>");
        lexer.scan(text);
        builder.appendHtmlConstant("</pre>");
        htmlPanel.add(new HTML(builder.toSafeHtml()));
    }

    private static boolean match(String s, int i, String p) {
        if (i < 0) {
            return false;
        }
        for (int j = 0; i < s.length() && j < p.length(); i++, j++) {
            if (s.charAt(i) != p.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIdentifierChar(char ch) {
        return ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ('0' <= ch && ch <= '9') || (ch == '_');
    }

    private void renderReservedWord(String token, long offset, SafeHtmlBuilder builder) {
        builder.appendHtmlConstant("<b>")
                .appendEscaped(token)
                .appendHtmlConstant("</b>");
    }

    private void renderIdentifier(String id, long offset, SafeHtmlBuilder builder) {
        // Use <span>id</span> as a place holder
        String elementId = HTMLPanel.createUniqueId();
        SafeHtml divTagStart = SafeHtmlUtils.fromTrustedString("<span id=\"" + elementId + "\">");
        builder.append(divTagStart)
                .appendEscaped(id)
                .appendHtmlConstant("</span>");
        tokenElementIds.put(offset, elementId);
    }

    private void renderComment(String comment, SafeHtmlBuilder builder) {
        builder.appendHtmlConstant("<i>");
        builder.appendEscaped(comment);
        builder.appendHtmlConstant("</i>");
    }

    private void renderStringLiteral(String s, SafeHtmlBuilder builder) {
        builder.appendEscaped(s);
    }

    private void renderUnknown(char c, SafeHtmlBuilder builder) {
        builder.appendEscaped(String.valueOf(c));
    }
}
