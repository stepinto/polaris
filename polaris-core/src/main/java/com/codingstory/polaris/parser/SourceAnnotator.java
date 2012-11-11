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
    private static final Comparator<Usage> USAGE_COMPARATOR = new Comparator<Usage>() {
        @Override
        public int compare(Usage left, Usage right) {
            return Longs.compare(left.getSpan().getFrom(), right.getSpan().getFrom());
        }
    };

    public static String annotate(InputStream in, List<Usage> usages) throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        List<Usage> sortedUsages = Lists.newArrayList(usages);
        Collections.sort(sortedUsages, USAGE_COMPARATOR);
        long i = 0;
        int j = 0;
        pw.print("<source>");
        sortedUsages= ImmutableList.copyOf(Iterables.filter(sortedUsages, new Predicate<Usage>() {
            @Override
            public boolean apply(Usage usage) {
                return accepts(usage);
            }
        }));

        int ch = in.read();
        while (ch != -1 && j < sortedUsages.size()) {
            Usage currentUsage = sortedUsages.get(j);
            long currentUsageFrom = currentUsage.getSpan().getFrom();
            if (i < currentUsageFrom) {
                pw.write(escape((char) ch));
                ch = in.read();
                i++;
            } else if (i > currentUsageFrom) {
                j++;
            } else {
                long currentUsageTo = currentUsage.getSpan().getTo();
                byte[] text = new byte [(int)(currentUsageTo - currentUsageFrom)];
                int k = 0;
                while (i < currentUsageTo && ch != -1) {
                    text[k] = (byte) ch;
                    ch = in.read();
                    k++;
                    i++;
                }
                emit(pw, new String(text), currentUsage);
                i = currentUsageTo;
            }
        }
        while (ch != -1) {
            pw.write(escape((char) ch));
            ch = in.read();
        }
        pw.print("</source>");
        pw.close();
        return sw.toString();
    }

    private static boolean accepts(Usage usage) {
        if (usage instanceof TypeUsage) {
            TypeUsage typeUsage = (TypeUsage) usage;
            // Type declaration's span is buggy. Don't generate tags for them until we move to antlr parser.
            return typeUsage.getKind() != TypeUsage.Kind.TYPE_DECLARATION;
        } else {
            return false;
        }
    }

    private static void emit(PrintWriter out, String text, Usage usage) {
        if (usage instanceof TypeUsage) {
            TypeUsage typeUsage = (TypeUsage) usage;
            TypeHandle type = typeUsage.getType();
            if (!TypeUtils.isPrimitiveTypeHandle(type)) {
                String typeNameStr = type.getName().toString();
                out.printf("<type-usage type=\"%s\" type-id=\"%d\" resolved=\"%s\">%s</type-usage>",
                        escape(typeNameStr), type.getId(), Boolean.toString(type.isResolved()), escape(text));
                return;
            }
            // Don't show links for primitive or unresolved types.
            out.print(escape(text));
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
