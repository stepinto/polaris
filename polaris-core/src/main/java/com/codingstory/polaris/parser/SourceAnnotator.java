package com.codingstory.polaris.parser;

import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

import java.io.IOException;
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

    public static String annotate(String source, List<Token> tokens) throws IOException {
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch > 127) {
                // TODO: We're not not supporting analysis on non-ASCII files yet.
                return "<source>" + source + "</source>";
            }
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        List<Token> sortedTokens = Lists.newArrayList(tokens);
        Collections.sort(sortedTokens, TOKEN_COMPARATOR);
        int i = 0;
        int j = 0;
        pw.print("<source>");
        while (i < source.length() && j < sortedTokens.size()) {
            Token currentToken = sortedTokens.get(j);
            long currentTokenFrom = currentToken.getSpan().getFrom();
            if (i < currentTokenFrom) {
                pw.write(escape(source.charAt(i)));
                i++;
            } else if (i == currentTokenFrom) {
                long currentTokenTo = currentToken.getSpan().getTo();
                if (currentTokenTo > source.length()) {
                    System.out.println("currentTokenTo = " + currentTokenTo);
                    System.out.println("currentToken = " + currentToken);
                    System.out.println("source = " + source);
                }
                if (emit(pw, source.substring((int) currentTokenFrom, (int) currentTokenTo), currentToken)) {
                    i = (int) currentTokenTo;
                }
                j++;
            } else {
                j++;
            }
        }
        pw.print("</source>");
        pw.close();
        return sw.toString();
    }

    private static boolean emit(PrintWriter out, String text, Token token) {
        if (token instanceof FieldDeclaration) {
            FieldDeclaration field = (FieldDeclaration) token;
            out.printf("<field-declaration field=\"%s\">%s</field-declaration>",
                    escape(field.getName().toString()), escape(text));
            return true;
        } else if (token instanceof MethodDeclaration) {
            return false; // TODO
//            MethodDeclaration method = (MethodDeclaration) token;
//            out.printf("<method-declaration method='%s'>%s</method-declaration>",
//                    method.getMethodName(), escapedText); // TOOD: Use its full name.
//            return true;
        } else if (token instanceof TypeUsage) {
            TypeReference type = ((TypeUsage) token).getTypeReference();
            String typeName;
            if (type.isResoleved()) {
                typeName = ((ResolvedTypeReference) type).getName().toString();
            } else {
                typeName = type.getUnqualifiedName();
            }
            out.printf("<type-usage type=\"%s\" resolved=\"%s\">%s</type-usage>",
                    escape(typeName), Boolean.toString(type.isResoleved()), escape(text));
            return true;
        } else {
            return false;
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
