package com.codingstory.polaris.parser;

import com.codingstory.polaris.SkipCheckingExceptionWrapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.TokenMgrError;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.Node;
import japa.parser.ast.body.AnnotationDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.type.Type;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class TokenExtractor {

    private static class ASTVisitor extends VoidVisitorAdapter {

        private final LineMonitorInputStream in;
        private final List<Token> results = Lists.newArrayList();
        private PackageDeclaration packageDeclaration;
        private final LinkedList<TypeDeclaration> typeDeclarationStack = Lists.newLinkedList();
        private final LinkedList<MethodDeclaration> methodDeclarationStack = Lists.newLinkedList();
        private final LinkedList<TypeTable<TypeReference>> typeTable = Lists.newLinkedList();
        private final List<ImportDeclaration> imports = Lists.newArrayList();

        private ASTVisitor(LineMonitorInputStream in) {
            this.in = in;
            typeTable.push(new TypeTable<TypeReference>()); // To hold imports
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
        public void visit(ImportDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            if (node.isAsterisk()) {
                imports.add(node);
            } else {
                TypeTable<TypeReference> symbolTable = Iterables.getOnlyElement(typeTable);
                FullyQualifiedName name = FullyQualifiedName.of(node.getName().toString());
                TypeReference typeReference = new UnresolvedTypeReferenece(ImmutableList.of(name));
                symbolTable.put(name, typeReference);
            }
            super.visit(node, arg);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            String javaDoc = null;
            if (node.getJavaDoc() != null) {
                javaDoc = node.getJavaDoc().toString();
            }
            TypeDeclaration typeDeclaration = TypeDeclaration.newBuilder()
                    .setKind(node.isInterface() ? Token.Kind.INTERFACE_DECLARATION : Token.Kind.CLASS_DECLARATION)
                    .setSpan(findTokenSpan(node))
                    .setName(FullyQualifiedName.of(findPackageName(), node.getName()))
                    .setJavaDocComment(javaDoc)
                    .build();
            results.add(typeDeclaration);
            typeDeclarationStack.push(typeDeclaration);
            super.visit(node, arg);
            typeDeclarationStack.pop();
        }

        @Override
        public void visit(AnnotationDeclaration node, Object arg) {
            // TODO: We temporarily treat annotations as classes
            Preconditions.checkNotNull(node);
            TypeDeclaration classDeclaration = TypeDeclaration.newBuilder()
                    .setKind(Token.Kind.ANNOTATION_DECLARATION)
                    .setSpan(findTokenSpan(node))
                    .setName(FullyQualifiedName.of(findPackageName(), node.getName()))
                    .build();
            results.add(classDeclaration);
            typeDeclarationStack.push(classDeclaration);
            Preconditions.checkNotNull(node);
            super.visit(node, arg);
            typeDeclarationStack.pop();
        }

        @Override
        public void visit(japa.parser.ast.body.EnumDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            TypeDeclaration enumDeclaration = TypeDeclaration.newBuilder()
                    .setKind(Token.Kind.ENUM_DECLARATION)
                    .setSpan(findTokenSpan(node))
                    .setName(FullyQualifiedName.of(findPackageName(), node.getName()))
                    .build();
            results.add(enumDeclaration);
            typeDeclarationStack.push(enumDeclaration);
            super.visit(node, arg);
            typeDeclarationStack.pop();
        }

        @Override
        public void visit(japa.parser.ast.body.MethodDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            MethodDeclaration methodDeclaration = MethodDeclaration.newBuilder()
                    .setSpan(findTokenSpan(node))
                    .setPackageName(findPackageName())
                    .setClassName(findClassName())
                    .setMethodName(node.getName())
                    .build();
            results.add(methodDeclaration);
            methodDeclarationStack.push(methodDeclaration);
            super.visit(node, arg);
            methodDeclarationStack.pop();
        }

        @Override
        public void visit(japa.parser.ast.body.FieldDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            TypeReference type = resolveType(node.getType());
            for (VariableDeclarator var : node.getVariables()) {
                FieldDeclaration field = FieldDeclaration.newBuilder()
                        .setSpan(findTokenSpan(var.getId()))
                        .setTypeReference(type)
                        .setPackageName(findPackageName())
                        .setClassName(findClassName())
                        .setVariableName(var.getId().getName())
                        .build();
                results.add(field);
            }
            super.visit(node, arg);
        }

        private String findClassName() {
            return typeDeclarationStack.getLast().getName().getTypeName();
        }

        private TypeReference resolveType(Type type) {
            String name = type.toString();
            TypeReference typeReference = PrimitiveTypeResolver.resolve(name);
            if (typeReference != null) {
                return typeReference;
            }
            typeReference = typeTable.getLast().resolve(name);
            if (typeReference != null) {
                return typeReference;
            }
            FullyQualifiedName qualifiedName = FullyQualifiedName.of(name);
            if (qualifiedName.hasPackageName()) {
                return new UnresolvedTypeReferenece(ImmutableList.of(qualifiedName));
            } else {
                // Assume the type is "Type", the package is "mypkg" and "import pkg1.*" and "import pkg2.*".
                // We will generate the following candidates:
                //   mypkg.Type
                //   Type
                //   pkg1.Type
                //   pkg2.Type
                List<FullyQualifiedName> candidates = Lists.newArrayList();
                candidates.add(FullyQualifiedName.of(findPackageName(), name));
                candidates.add(FullyQualifiedName.of(null, name));
                for (ImportDeclaration imp : imports) {
                    if (imp.isAsterisk()) {
                        String packageName = imp.getName().getName();
                        candidates.add(FullyQualifiedName.of(packageName, name));
                    }
                }
                return new UnresolvedTypeReferenece(candidates);
            }
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


    public static List<Token> extract(InputStream in) throws IOException {
        Preconditions.checkNotNull(in);
        try {
            LineMonitorInputStream lmin = new LineMonitorInputStream(in);
            CompilationUnit compilationUnit = JavaParser.parse(lmin);
            ASTVisitor visitor = new ASTVisitor(lmin);
            visitor.visit(compilationUnit, null);
            return visitor.getResults();
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
}
