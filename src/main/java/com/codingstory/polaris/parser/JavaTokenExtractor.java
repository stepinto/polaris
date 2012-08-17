package com.codingstory.polaris.parser;

import com.google.common.collect.Lists;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class JavaTokenExtractor {

    private static class ASTVisitor extends VoidVisitorAdapter {

        private List<Token> tokens = Lists.newArrayList();

        @Override
        public void visit(japa.parser.ast.PackageDeclaration node, Object arg) {
            super.visit(node, arg);
            tokens.add(new PackageDeclaration(node.getName().getName()));
        }

        public List<Token> getTokens() { return tokens; }
    }

    private InputStream in;

    public JavaTokenExtractor setInputStream(InputStream in) {
        this.in = in;
        return this;
    }

    public List<Token> extractTerms() throws IOException {
        try {
            CompilationUnit compilationUnit = JavaParser.parse(in);
            ASTVisitor visitor = new ASTVisitor();
            visitor.visit(compilationUnit, null);
            return visitor.getTokens();
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

}
