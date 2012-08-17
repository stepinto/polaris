package com.codingstory.polaris.parser;

import com.google.common.collect.Lists;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class JavaTermExtractor {

    private static class ASTVisitor extends VoidVisitorAdapter {

        private List<Term> terms = Lists.newArrayList();

        @Override
        public void visit(japa.parser.ast.PackageDeclaration node, Object arg) {
            super.visit(node, arg);
            terms.add(new PackageDeclaration(node.getName().getName()));
        }

        public List<Term> getTerms() { return terms; }
    }

    private InputStream in;

    public JavaTermExtractor setInputStream(InputStream in) {
        this.in = in;
        return this;
    }

    public List<Term> extractTerms() throws IOException {
        try {
            CompilationUnit compilationUnit = JavaParser.parse(in);
            ASTVisitor visitor = new ASTVisitor();
            visitor.visit(compilationUnit, null);
            return visitor.getTerms();
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

}
