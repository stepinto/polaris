package com.codingstory.polaris.parser;

import com.codingstory.polaris.SkipCheckingExceptionWrapper;
import com.codingstory.polaris.parser.ParserProtos.FileHandle;
import com.codingstory.polaris.parser.ParserProtos.JumpTarget;
import com.codingstory.polaris.parser.ParserProtos.Position;
import com.codingstory.polaris.parser.ParserProtos.Span;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.TokenMgrError;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.visitor.VoidVisitor;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static com.codingstory.polaris.parser.TypeUtils.positionOf;
import static com.codingstory.polaris.parser.TypeUtils.spanOf;

public final class ParserUtils {
    private ParserUtils() {}

    public static void safeVisit(String s, VoidVisitor<?> visitor) throws IOException {
        try {
            CompilationUnit compilationUnit;
            synchronized (ParserUtils.class) {
                // Walk around race condition bug in JavaParser. TODO: Fix it upstream.
                compilationUnit = JavaParser.parse(new StringReader(s));
            }
            visitor.visit(compilationUnit, null);
        } catch (ParseException e) {
            throw new IOException(e);
        } catch (TokenMgrError e) {
            // Don't know why this error may be raised when parsing Eclipse code.
            throw new IOException(e);
        } catch (StackOverflowError e) {
            throw new IOException(e); // See issue #2.
        } catch (Error e) {
            // The parser may throw java.lang.Error, e.g. at JavaCharStream.java:347.
            throw new IOException(e);
        } catch (SkipCheckingExceptionWrapper e) {
            throw (IOException) e.getCause();
        }
    }

    public static Span nodeSpan(Node node) {
        Preconditions.checkNotNull(node);
        Position from = positionOf(node.getBeginLine() - 1, node.getBeginColumn() - 1);
        Position to = positionOf(node.getEndLine() - 1, node.getEndColumn());
        return spanOf(from, to);
    }

    public static JumpTarget nodeJumpTarget(FileHandle file, Node node) {
        return JumpTarget.newBuilder()
                .setFile(Preconditions.checkNotNull(file))
                .setSpan(nodeSpan(node))
                .build();
    }

    public static String makeTypeName(String pkg, List<String> outerClasses, String innerClass) {
        Preconditions.checkNotNull(outerClasses);
        Preconditions.checkNotNull(innerClass);
        List<String> parts = Lists.newArrayListWithCapacity(outerClasses.size() + 2);
        if (!Strings.isNullOrEmpty(pkg)) {
            parts.add(pkg);
        }
        parts.addAll(outerClasses);
        parts.add(innerClass);
        return Joiner.on(".").join(parts);
    }

    /** Drops any generic types from a type name. For example, it returns "List" if passing "List<Integer>". */
    public static String dropGenericTypes(String typeName) {
        int p = typeName.indexOf('<');
        if (p == -1) {
            return typeName;
        }
        return typeName.substring(0, p);
    }

}
