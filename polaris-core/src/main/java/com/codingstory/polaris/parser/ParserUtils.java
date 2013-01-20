package com.codingstory.polaris.parser;

import com.codingstory.polaris.SkipCheckingExceptionWrapper;
import com.google.common.base.Preconditions;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.TokenMgrError;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.visitor.VoidVisitor;

import java.io.IOException;
import java.io.InputStream;

public final class ParserUtils {
    private ParserUtils() {}

    public static void safeVisit(InputStream in, VoidVisitor<?> visitor) throws IOException {
        try {
            CompilationUnit compilationUnit = JavaParser.parse(in);
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
        Position from = new Position(node.getBeginLine() - 1, node.getBeginColumn() - 1);
        Position to = new Position(node.getEndLine() - 1, node.getEndColumn());
        return new Span(from, to);
    }
}
