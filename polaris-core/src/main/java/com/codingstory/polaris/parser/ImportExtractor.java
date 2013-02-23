package com.codingstory.polaris.parser;

import com.google.common.collect.Lists;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Extract possible imports, including implicit import, e.g. same package.
 */
public class ImportExtractor {

    public static class Result {
        private final String pkg;
        private final List<String> classes;
        private final List<String> packages;

        public Result(String pkg, List<String> classes, List<String> packages) {
            this.pkg = pkg;
            this.classes = classes;
            this.packages = packages;
        }

        public String getPackage() {
            return pkg;
        }

        public List<String> getImportedClasses() {
            return classes;
        }

        public List<String> getImportedPackages() {
            return packages;
        }
    }

    private static class ImportExtractVisitor extends VoidVisitorAdapter<Void> {
        private String pkg = "";
        private final List<String> classes = Lists.newArrayList();
        private final List<String> packages = Lists.newArrayList();

        @Override
        public void visit(PackageDeclaration n, Void arg) {
            super.visit(n, arg);
            pkg = n.getName().toString();
        }

        @Override
        public void visit(ImportDeclaration n, Void arg) {
            super.visit(n, arg);
            String name = n.getName().toString();
            if (n.isAsterisk()) {
                packages.add(name);
            } else {
                classes.add(name);
            }
        }

        public Result getResult() {
            return new Result(pkg, classes,  packages);
        }
    }

    public static Result findImports(String source) throws IOException {
        ImportExtractVisitor visitor = new ImportExtractVisitor();
        ParserUtils.safeVisit(source, visitor);
        return visitor.getResult();
    }
}
