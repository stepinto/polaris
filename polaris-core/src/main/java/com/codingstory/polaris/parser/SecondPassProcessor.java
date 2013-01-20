package com.codingstory.polaris.parser;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.IdUtils;
import com.codingstory.polaris.JumpTarget;
import com.codingstory.polaris.SkipCheckingExceptionWrapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.AnnotationDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.InitializerDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import static com.codingstory.polaris.parser.ParserUtils.nodeJumpTarget;
import static com.codingstory.polaris.parser.ParserUtils.nodeSpan;

public final class SecondPassProcessor {

    public static class Result {
        private final List<ClassType> classTypes;
        private final List<Usage> usages;

        public Result(List<ClassType> classTypes, List<Usage> usages) {
            this.classTypes = Preconditions.checkNotNull(classTypes);
            this.usages = Preconditions.checkNotNull(usages);
        }

        public List<ClassType> getClassTypes() {
            return classTypes;
        }

        public List<Usage> getUsages() {
            return usages;
        }
    }

    private static class ASTVisitor extends VoidVisitorAdapter {
        private final String project;
        private final long fileId;
        private final SymbolTable symbolTable;
        private final List<Usage> usages = Lists.newArrayList();
        private final List<ClassType> discoveredTypes = Lists.newArrayList();
        private final String pkg;
        private final LinkedList<ClassType> typeStack = Lists.newLinkedList();
        private final LinkedList<Method> methodStack = Lists.newLinkedList();
        private final List<ImportDeclaration> imports = Lists.newArrayList();
        private final IdGenerator idGenerator;

        /** Symbol table for classTypes. */
        // TODO: symbol table for variables

        private ASTVisitor(String project, long fileId,
                SymbolTable symbolTable,
                IdGenerator idGenerator,
                String pkg) {
            this.project = Preconditions.checkNotNull(project);
            this.fileId = IdUtils.checkValid(fileId);
            this.symbolTable = Preconditions.checkNotNull(symbolTable);
            this.idGenerator = Preconditions.checkNotNull(idGenerator);
            this.pkg = Preconditions.checkNotNull(pkg);
        }

        @Override
        public void visit(japa.parser.ast.PackageDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            super.visit(node, arg);
        }

        @Override
        public void visit(ImportDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            if (node.isAsterisk()) {
                symbolTable.registerImportPackage(node.getName().toString());
            } else {
                FullTypeName name = FullTypeName.of(node.getName().toString());
                TypeHandle clazz = symbolTable.resolveTypeHandle(name);
                usages.add(new TypeUsage(clazz, nodeJumpTarget(fileId, node), TypeUsage.Kind.IMPORT));
                symbolTable.registerImportClass(clazz);
            }
            super.visit(node, arg);
        }

        @Override
        public void visit(final ClassOrInterfaceDeclaration node, final Object arg) {
            Preconditions.checkNotNull(node);
            List<ClassOrInterfaceType> superTypeAsts = ImmutableList.copyOf(Iterables.concat(
                    nullToEmptyList(node.getExtends()), nullToEmptyList(node.getImplements())));
            processTypeDeclaration(node.getName(),
                    node.isInterface() ? ClassType.Kind.INTERFACE : ClassType.Kind.CLASS,
                    nodeJumpTarget(fileId, node),
                    superTypeAsts,
                    node.getJavaDoc() != null ? node.getJavaDoc().getContent() : null,
                    new Runnable() {
                        @Override
                        public void run() {
                            ASTVisitor.super.visit(node, arg);
                        }
                    });
        }

        private void processTypeDeclaration(String name, ClassType.Kind kind, JumpTarget jumpTarget,
                List<ClassOrInterfaceType> superTypeAsts, String javaDoc, Runnable visitChildren) {
            FullTypeName fullTypeName = FullTypeName.of(pkg, makeTypeName(name));
            ClassType clazz = symbolTable.lookUpClassType(fullTypeName);
            if (clazz == null) {
                throw new AssertionError(fullTypeName + " should have been identified in 1st pass");
            }
            for (ClassOrInterfaceType superTypeAst : superTypeAsts) {
                String superTypeName = superTypeAst.toString();
                TypeHandle superType = symbolTable.resolveTypeHandle(FullTypeName.of(superTypeName));
                usages.add(new TypeUsage(superType, nodeJumpTarget(fileId, superTypeAst), TypeUsage.Kind.SUPER_CLASS));
                clazz.addSuperType(superType);
            }
            usages.add(new TypeUsage(clazz.getHandle(), jumpTarget, TypeUsage.Kind.TYPE_DECLARATION));
            discoveredTypes.add(clazz);
            symbolTable.enterScope();
            typeStack.push(clazz);
            visitChildren.run();
            typeStack.pop();
            symbolTable.leaveScope();
        }

        private String makeTypeName(String name) {
            if (typeStack.isEmpty()) {
                return name;
            } else {
                return currentType().getName().getTypeName() + "$" + name;
            }
        }

        @Override
        public void visit(final AnnotationDeclaration node, final Object arg) {
            Preconditions.checkNotNull(node);
            processTypeDeclaration(node.getName(),
                    ClassType.Kind.ANNOTATION,
                    nodeJumpTarget(fileId, node),
                    ImmutableList.<ClassOrInterfaceType>of(),
                    node.getJavaDoc() != null ? node.getJavaDoc().getContent() : null,
                    new Runnable() {
                        @Override
                        public void run() {
                            ASTVisitor.super.visit(node, arg);
                        }
                    });
        }

        @Override
        public void visit(final japa.parser.ast.body.EnumDeclaration node, final Object arg) {
            Preconditions.checkNotNull(node);
            processTypeDeclaration(node.getName(),
                    ClassType.Kind.ENUM,
                    nodeJumpTarget(fileId, node),
                    nullToEmptyList(node.getImplements()),
                    node.getJavaDoc() != null ? node.getJavaDoc().getContent() : null,
                    new Runnable() {
                        @Override
                        public void run() {
                            ASTVisitor.super.visit(node, arg);
                        }
                    });
        }

        @Override
        public void visit(final japa.parser.ast.body.MethodDeclaration node, final Object arg) {
            Preconditions.checkNotNull(node);
            processMethodDeclaration(node.getType(), node.getName(), nodeJumpTarget(fileId, node),
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
            processMethodDeclaration(null, "<init>", nodeJumpTarget(fileId, node),
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
            processMethodDeclaration(null, "<cinit>", nodeJumpTarget(fileId, node),
                    null, null, new Runnable() {
                @Override
                public void run() {
                    ASTVisitor.super.visit(node, arg);
                }
            });
        }

        private void processMethodDeclaration(
                japa.parser.ast.type.Type type,
                String methodName,
                JumpTarget jumpTarget,
                List<Parameter> methodParameters,
                List<NameExpr> methodThrows,
                Runnable visitChildren) {
            TypeHandle returnType;
            if (type == null) {
                returnType = PrimitiveType.VOID.getHandle();
            } else {
                returnType = symbolTable.resolveTypeHandle(FullTypeName.of(type.toString()));
                usages.add(new TypeUsage(returnType, nodeJumpTarget(fileId, type), TypeUsage.Kind.METHOD_SIGNATURE));
            }
            List<Method.Parameter> parameters = Lists.newArrayList();
            List<TypeHandle> parameterTypes = Lists.newArrayList();
            for (Parameter parameter : nullToEmptyList(methodParameters)) {
                TypeHandle parameterType = symbolTable.resolveTypeHandle(FullTypeName.of(parameter.getType().toString()));
                usages.add(new TypeUsage(parameterType, nodeJumpTarget(fileId, parameter.getType()),
                        TypeUsage.Kind.METHOD_SIGNATURE));
                parameterTypes.add(parameterType);
                parameters.add(new Method.Parameter(parameterType, parameter.getId().getName()));
            }
            List<TypeHandle> exceptions = Lists.newArrayList();
            for (NameExpr throwExpr : nullToEmptyList(methodThrows)) {
                TypeHandle exceptionType = symbolTable.resolveTypeHandle(FullTypeName.of(throwExpr.getName()));
                usages.add(new TypeUsage(
                        exceptionType, nodeJumpTarget(fileId, throwExpr), TypeUsage.Kind.METHOD_SIGNATURE));
                exceptions.add(exceptionType);
            }
            FullMemberName fullMemberName = FullMemberName.of(currentType().getName(), methodName);
            Method method = new Method(
                    new MethodHandle(generateId(), fullMemberName, parameterTypes),
                    returnType,
                    parameters,
                    exceptions,
                    EnumSet.noneOf(Modifier.class),
                    jumpTarget);
            usages.add(new MethodUsage(method.getHandle(), jumpTarget, MethodUsage.Kind.METHOD_DECLARATION));
            currentType().getMethods().add(method);
            methodStack.push(method);
            visitChildren.run();
            methodStack.pop();
        }

        @Override
        public void visit(japa.parser.ast.body.FieldDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            TypeHandle type = symbolTable.resolveTypeHandle(FullTypeName.of(node.getType().toString()));
            usages.add(new TypeUsage(type, nodeJumpTarget(fileId, node.getType()), TypeUsage.Kind.FIELD));
            for (VariableDeclarator varDecl : node.getVariables()) {
                JumpTarget fieldTarget = nodeJumpTarget(fileId, varDecl.getId());
                FullMemberName fullMemberName = FullMemberName.of(currentType().getName(), varDecl.getId().getName());
                Field field = new Field(
                        new FieldHandle(generateId(), fullMemberName),
                        type,
                        EnumSet.noneOf(Modifier.class),
                        fieldTarget);
                currentType().getFields().add(field);
                usages.add(new FieldUsage(field.getHandle(), fieldTarget, FieldUsage.Kind.FIELD_DECLARATION));
            }
            super.visit(node, arg);
        }

        @Override
        public void visit(VariableDeclarationExpr node, Object arg) {
            Preconditions.checkNotNull(node);
            TypeHandle type = symbolTable.resolveTypeHandle(FullTypeName.of(node.getType().toString()));
            usages.add(new TypeUsage(
                    type, nodeJumpTarget(fileId, node.getType()), TypeUsage.Kind.LOCAL_VARIABLE));
            super.visit(node, arg);
        }

        private ClassType currentType() {
            return typeStack.getFirst();
        }

        private Method currentMethod() {
            return methodStack.getLast();
        }

        private String findMethodName() {
            return currentMethod().getName().toString();
        }

        public List<Usage> getUsages() { return usages; }

        public List<ClassType> getClassTypes() { return discoveredTypes; }

        private <T> List<T> nullToEmptyList(List<T> list) {
            return list == null ? ImmutableList.<T>of() : list;
        }

        private long generateId() {
            try {
                return idGenerator.next();
            } catch (IOException e) {
                throw new SkipCheckingExceptionWrapper(e);
            }
        }
    }

    private SecondPassProcessor() {}

    public static Result extract(String project,
            long fileId,
            InputStream in,
            SymbolTable symbolTable,
            IdGenerator idGenerator,
            String pkg) throws IOException {
        Preconditions.checkNotNull(project);
        IdUtils.checkValid(fileId);
        Preconditions.checkNotNull(in);
        Preconditions.checkNotNull(symbolTable);
        Preconditions.checkNotNull(idGenerator);
        symbolTable.enterCompilationUnit(pkg);
        ASTVisitor visitor = new ASTVisitor(project, fileId, symbolTable, idGenerator, pkg);
        ParserUtils.safeVisit(in, visitor);
        symbolTable.leaveCompilationUnit();
        return new Result(visitor.getClassTypes(), visitor.getUsages());
    }
}
