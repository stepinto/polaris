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
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.InitializerDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.Type;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public final class TokenExtractor {

    private static class ASTVisitor extends VoidVisitorAdapter {

        private final LineMonitorInputStream in;
        private final TypeResolver externalTypeResolver;
        private final List<Token> results = Lists.newArrayList();
        private PackageDeclaration packageDeclaration;
        private final LinkedList<TypeDeclaration> typeDeclarationStack = Lists.newLinkedList();
        private final LinkedList<MethodDeclaration> methodDeclarationStack = Lists.newLinkedList();
        private final List<ImportDeclaration> imports = Lists.newArrayList();

        /** Symbol table for types. */
        private final TypeTable<TypeReference> typeTable = new TypeTable<TypeReference>();
        // TODO: symbol table for variables

        private ASTVisitor(LineMonitorInputStream in, TypeResolver externalTypeResolver) {
            this.in = in;
            this.externalTypeResolver = externalTypeResolver;
            typeTable.enterFrame(); // a frame for imports
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
                FullyQualifiedTypeName name = FullyQualifiedTypeName.of(node.getName().toString());
                UnresolvedTypeReferenece unresolved = new UnresolvedTypeReferenece(ImmutableList.of(name));
                ResolvedTypeReference resolved = externalTypeResolver.resolve(unresolved);
                typeTable.put(name, resolved != null ? resolved : unresolved);
                results.add(TypeUsage.newBuilder()
                        .setTypeReference(resolved != null ? resolved : unresolved)
                        .setSpan(findTokenSpan(node.getName()))
                        .build());
            }
            super.visit(node, arg);
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
                    .setName(FullyQualifiedTypeName.of(findPackageName(), node.getName()))
                    .setJavaDocComment(javaDoc)
                    .build();
            results.add(typeDeclaration);
            for (ClassOrInterfaceType superType : Iterables.concat(
                    nullToEmptyList(node.getExtends()),
                    nullToEmptyList(node.getImplements()))) {
                TypeUsage usage = buildTypeUsage(superType);
                results.add(usage);
            }
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
                    .setName(FullyQualifiedTypeName.of(findPackageName(), node.getName()))
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
                    .setName(FullyQualifiedTypeName.of(findPackageName(), node.getName()))
                    .build();
            results.add(enumDeclaration);
            typeDeclarationStack.push(enumDeclaration);
            super.visit(node, arg);
            typeDeclarationStack.pop();
        }

        @Override
        public void visit(final japa.parser.ast.body.MethodDeclaration node, final Object arg) {
            Preconditions.checkNotNull(node);
            processMethodDeclaration(node.getType(), node.getName(), findTokenSpan(node),
                    node.getParameters(), node.getThrows(), new Runnable() {
                        @Override
                        public void run() {
                            ASTVisitor.super.visit(node, arg);
                        }
                    });
        }

        @Override
        public void visit(final ConstructorDeclaration node, final Object arg) {
            Preconditions.checkNotNull(node);
            processMethodDeclaration(null, "<init>", findTokenSpan(node),
                    node.getParameters(), node.getThrows(), new Runnable() {
                @Override
                public void run() {
                    ASTVisitor.super.visit(node, arg);
                }
            });
        }

        @Override
        public void visit(final InitializerDeclaration node, final Object arg) {
            Preconditions.checkNotNull(node);
            processMethodDeclaration(null, "<cinit>", findTokenSpan(node),
                    null, null, new Runnable() {
                @Override
                public void run() {
                    ASTVisitor.super.visit(node, arg);
                }
            });
        }

        private void processMethodDeclaration(
                Type type,
                String methodName,
                Token.Span span,
                List<Parameter> methodParameters,
                List<NameExpr> methodThrows,
                Runnable visitChildren) {
            TypeReference returnType = resolveType(type == null ? "void" : type.toString());
            if (type != null) {
                results.add(TypeUsage.newBuilder()
                        .setSpan(findTokenSpan(type))
                        .setTypeReference(returnType)
                        .build());
            }
            List<MethodDeclaration.Parameter> parameters = Lists.newArrayList();
            for (Parameter parameter : nullToEmptyList(methodParameters)) {
                TypeReference parameterType = resolveType(parameter.getType().toString());
                results.add(TypeUsage.newBuilder()
                        .setSpan(findTokenSpan(parameter.getType()))
                        .setTypeReference(parameterType)
                        .build());
                parameters.add(new MethodDeclaration.Parameter(parameterType, parameter.getId().getName()));
            }
            List<TypeReference> exceptionTypes = Lists.newArrayList();
            for (NameExpr throwExpr : nullToEmptyList(methodThrows)) {
                TypeReference exceptionType = resolveType(throwExpr.getName());
                results.add(TypeUsage.newBuilder()
                        .setSpan(findTokenSpan(throwExpr))
                        .setTypeReference(exceptionType)
                        .build());
                exceptionTypes.add(exceptionType);
            }
            MethodDeclaration methodDeclaration = MethodDeclaration.newBuilder()
                    .setSpan(span)
                    .setPackageName(findPackageName())
                    .setClassName(findClassName())
                    .setMethodName(methodName)
                    .setReturnType(returnType)
                    .setParameters(parameters)
                    .setExceptions(exceptionTypes)
                    .build();
            results.add(methodDeclaration);
            methodDeclarationStack.push(methodDeclaration);
            visitChildren.run();
            methodDeclarationStack.pop();
        }

        @Override
        public void visit(japa.parser.ast.body.FieldDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            TypeReference type = resolveType(node.getType().toString());
            TypeUsage typeUsage = TypeUsage.newBuilder()
                    .setSpan(findTokenSpan(node.getType()))
                    .setTypeReference(type)
                    .build();
            results.add(typeUsage);
            for (VariableDeclarator varDecl : node.getVariables()) {
                FieldDeclaration field = FieldDeclaration.newBuilder()
                        .setSpan(findTokenSpan(varDecl.getId()))
                        .setTypeReference(type)
                        .setName(FullMemberName.of(findPackageName(), findClassName(), varDecl.getId().getName()))
                        .build();
                results.add(field);
            }
            super.visit(node, arg);
        }

        @Override
        public void visit(VariableDeclarationExpr node, Object arg) {
            Preconditions.checkNotNull(node);
            TypeReference type = resolveType(node.getType().toString());
            TypeUsage typeUsage = TypeUsage.newBuilder()
                    .setSpan(findTokenSpan(node.getType()))
                    .setTypeReference(type)
                    .build();
            results.add(typeUsage);
            for (VariableDeclarator varDecl : node.getVars()) {
                LocalVariableDeclaration var = LocalVariableDeclaration.newBuilder()
                        .setSpan(findTokenSpan(varDecl))
                        .setTypeReference(type)
                        .setName(FullLocalName.of(findPackageName(), findClassName(),
                                findMethodName(), varDecl.getId().getName()))
                        .build();
                results.add(var);
            }
            // TODO: process variable declarations
            super.visit(node, arg);
        }

        private String findClassName() {
            return typeDeclarationStack.getLast().getName().getTypeName();
        }

        private TypeUsage buildTypeUsage(Node type) {
            return TypeUsage.newBuilder()
                    .setSpan(findTokenSpan(type))
                    .setTypeReference(resolveType(type.toString()))
                    .build();
        }

        private TypeReference resolveType(String name) {
            TypeReference typeReference = PrimitiveTypeResolver.resolve(name);
            if (typeReference != null) {
                return typeReference;
            }
            typeReference = typeTable.lookUp(name);
            if (typeReference != null) {
                return typeReference;
            }
            FullyQualifiedTypeName qualifiedName = FullyQualifiedTypeName.of(name);
            UnresolvedTypeReferenece unresolved;
            if (qualifiedName.hasPackageName()) {
                unresolved = new UnresolvedTypeReferenece(ImmutableList.of(qualifiedName));
            } else {
                // Assume the type is "Type", the package is "mypkg" and "import pkg1.*" and "import pkg2.*".
                // We will generate the following candidates:
                //   mypkg.Type
                //   Type
                //   pkg1.Type
                //   pkg2.Type
                List<FullyQualifiedTypeName> candidates = Lists.newArrayList();
                candidates.add(FullyQualifiedTypeName.of(findPackageName(), name));
                candidates.add(FullyQualifiedTypeName.of(null, name));
                for (ImportDeclaration imp : imports) {
                    if (imp.isAsterisk()) {
                        String packageName = imp.getName().getName();
                        candidates.add(FullyQualifiedTypeName.of(packageName, name));
                    }
                }
                unresolved = new UnresolvedTypeReferenece(candidates);
            }
            ResolvedTypeReference resolved = externalTypeResolver.resolve(unresolved);
            return resolved != null ? resolved : unresolved;
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

        private String findMethodName() {
            return methodDeclarationStack.getLast().getMethodName();
        }

        public List<Token> getResults() { return results; }

        private <T> List<T> nullToEmptyList(List<T> list) {
            return list == null ? ImmutableList.<T>of() : list;
        }
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

    private TokenExtractor() {}

    public static List<Token> extract(InputStream in, TypeResolver externalTypeResolver) throws IOException {
        Preconditions.checkNotNull(in);
        Preconditions.checkNotNull(externalTypeResolver);
        try {
            LineMonitorInputStream lmin = new LineMonitorInputStream(in);
            CompilationUnit compilationUnit = JavaParser.parse(lmin);
            ASTVisitor visitor = new ASTVisitor(lmin, externalTypeResolver);
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
