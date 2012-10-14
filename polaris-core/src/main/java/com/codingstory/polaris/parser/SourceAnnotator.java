package com.codingstory.polaris.parser;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SourceAnnotator {
    private static final Comparator<Token> TOKEN_COMPARATOR = new Comparator<Token>() {
        @Override
        public int compare(Token left, Token right) {
            return Longs.compare(left.getSpan().getFrom(), right.getSpan().getFrom());
        }
    };

    public static String annotate(InputStream in, List<Token> tokens) throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        List<Token> sortedTokens = Lists.newArrayList(tokens);
        Collections.sort(sortedTokens, TOKEN_COMPARATOR);
        long i = 0;
        int j = 0;
        pw.print("<source>");
        sortedTokens = ImmutableList.copyOf(Iterables.filter(sortedTokens, new Predicate<Token>() {
            @Override
            public boolean apply(Token token) {
                return accepts(token);
            }
        }));

        int ch = in.read();
        while (ch != -1 && j < sortedTokens.size()) {
            Token currentToken = sortedTokens.get(j);
            long currentTokenFrom = currentToken.getSpan().getFrom();
            if (i < currentTokenFrom) {
                pw.write(escape((char) ch));
                ch = in.read();
                i++;
            } else if (i > currentTokenFrom) {
                j++;
            } else {
                long currentTokenTo = currentToken.getSpan().getTo();
                byte[] text = new byte [(int)(currentTokenTo - currentTokenFrom)];
                int k = 0;
                while (i < currentTokenTo && ch != -1) {
                    text[k] = (byte) ch;
                    ch = in.read();
                    k++;
                    i++;
                }
                assert ch != -1;
                emit(pw, new String(text), currentToken);
                i = currentTokenTo;
            }
        }
        pw.print("</source>");
        pw.close();
        return sw.toString();
    }

    private static boolean accepts(Token token) {
        return token instanceof FieldDeclaration || token instanceof TypeUsage;
    }

    private static void emit(PrintWriter out, String text, Token token) {
        if (token instanceof FieldDeclaration) {
            FieldDeclaration field = (FieldDeclaration) token;
            out.printf("<field-declaration field=\"%s\">%s</field-declaration>",
                    escape(field.getName().toString()), escape(text));
        } else if (token instanceof TypeUsage) {
            TypeReference type = ((TypeUsage) token).getTypeReference();
            if (type.isResoleved()) {
                ResolvedTypeReference resolvedType = (ResolvedTypeReference) type;
                if (!ResolvedTypeReference.PRIMITIVES.contains(resolvedType)) {
                    String typeName = ((ResolvedTypeReference) type).getName().toString();
                    out.printf("<type-usage type=\"%s\" resolved=\"%s\">%s</type-usage>",
                            escape(typeName), Boolean.toString(type.isResoleved()), escape(text));
                    return;
                }
            }
            // Don't show links for primitive or unresolved types.
            out.printf(escape(type.getUnqualifiedName()));
        }
    }

    private static String escape(String s) {
        StringBuffer t = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            t.append(escape(ch));
        }
        return t.toString();
    }

    private static String escape(char ch) {
        if (ch == '&') {
            return "&amp;";
        } else if (ch == '<') {
            return "&lt;";
        } else if (ch == '>') {
            return "&gt;";
        } else if (ch == '\"') {
            return "&quot;";
        } else if (ch == '\'') {
            return "&apos;";
        } else {
            return String.valueOf(ch);
        }
    }
}
