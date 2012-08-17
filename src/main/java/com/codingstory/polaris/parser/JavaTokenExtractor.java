package com.codingstory.polaris.parser;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class JavaTokenExtractor {

    private static class ASTVisitor extends VoidVisitorAdapter {

        private final LineMonitorInputStream in;
        private final List<Token> results = Lists.newArrayList();
        private PackageDeclaration packageDeclaration;
        private final LinkedList<ClassDeclaration> classDeclarationStack = Lists.newLinkedList();
        private final LinkedList<MethodDeclaration> methodDeclarationStack = Lists.newLinkedList();

        private ASTVisitor(LineMonitorInputStream in) {
            this.in = in;
        }

        @Override
        public void visit(japa.parser.ast.PackageDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            this.packageDeclaration = PackageDeclaration.newBuilder()
                    .setSpan(findTokenSpan(node))
                    .setPackageName(node.getName().toString())
                    .build();
            results.add(packageDeclaration);
            super.visit(node, arg);
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            ClassDeclaration classDeclaration = ClassDeclaration.newBuilder()
                    .setSpan(findTokenSpan(node))
                    .setPackageName(findPackageName())
                    .setClassName(node.getName())
                    .build();
            results.add(classDeclaration);
            classDeclarationStack.push(classDeclaration);
            super.visit(node, arg);
            classDeclarationStack.pop();
        }

        @Override
        public void visit(japa.parser.ast.body.MethodDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            MethodDeclaration methodDeclaration = MethodDeclaration.newBuilder()
                    .setSpan(findTokenSpan(node))
                    .setPackageName(findPackageName())
                    .setClassName(classDeclarationStack.getLast().getClassName())
                    .setMethodName(node.getName())
                    .build();
            results.add(methodDeclaration);
            methodDeclarationStack.push(methodDeclaration);
            super.visit(node, arg);
            methodDeclarationStack.pop();
        }

        private Token.Span findTokenSpan(Node node) {
            Preconditions.checkNotNull(node);
            long from = in.translateLineColumnToOffset(node.getBeginLine() - 1, node.getBeginColumn() - 1);
            long to = in.translateLineColumnToOffset(node.getEndLine() - 1, node.getEndColumn() - 1) + 1;
            return Token.Span.of(from, to);
        }

        private String findPackageName() {
            return packageDeclaration == null ? null : packageDeclaration.getPackageName();
        }

        public List<Token> getResults() { return results; }
    }

    @VisibleForTesting
    static class LineMonitorInputStream extends FilterInputStream {

        private final List<Integer> lengths = Lists.newArrayList();
        private int currentLineLength = 0;

        public LineMonitorInputStream(InputStream in) {
            super(in);
        }

        public long translateLineColumnToOffset(int line, int col) {
            Preconditions.checkArgument(line >= 0);
            Preconditions.checkArgument(line <= lengths.size());
            Preconditions.checkArgument(col >= 0);
            long offset = col;
            for (int i = 0; i < line; i++) {
                offset += lengths.get(i);
            }
            return offset;
        }

        @Override
        public int read() throws IOException {
            int ch = super.read();    //To change body of overridden methods use File | Settings | File Templates.
            if (ch >= 0) {
                process((byte) ch);
            }
            return ch;
        }

        @Override
        public int read(byte[] bytes) throws IOException {
            int n = super.read(bytes);
            for (int i = 0; i < n; i++) {
                process(bytes[i]);
            }
            return n;
        }

        @Override
        public int read(byte[] bytes, int offset, int len) throws IOException {
            int n = super.read(bytes, offset, len);
            for (int i = 0; i < n; i++) {
                process(bytes[i]);
            }
            return n;
        }

        private void process(byte ch) throws IOException {
            if (ch == '\n') {
                lengths.add(currentLineLength + 1); // +1 for '\n'
                currentLineLength = 0;
            } else {
                currentLineLength++;
            }
        }
    }

    private InputStream in;

    public JavaTokenExtractor setInputStream(InputStream in) {
        Preconditions.checkNotNull(in);
        this.in = in;
        return this;
    }

    public List<Token> extractTokens() throws IOException {
        try {
            LineMonitorInputStream lmin = new LineMonitorInputStream(in);
            CompilationUnit compilationUnit = JavaParser.parse(lmin);
            ASTVisitor visitor = new ASTVisitor(lmin);
            visitor.visit(compilationUnit, null);
            return visitor.getResults();
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }
}
