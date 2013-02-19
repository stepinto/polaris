package com.codingstory.polaris.parser;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.SkipCheckingExceptionWrapper;
import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.ClassTypeHandle;
import com.codingstory.polaris.parser.ParserProtos.Field;
import com.codingstory.polaris.parser.ParserProtos.FieldHandle;
import com.codingstory.polaris.parser.ParserProtos.FieldUsage;
import com.codingstory.polaris.parser.ParserProtos.FileHandle;
import com.codingstory.polaris.parser.ParserProtos.JumpTarget;
import com.codingstory.polaris.parser.ParserProtos.Method;
import com.codingstory.polaris.parser.ParserProtos.MethodHandle;
import com.codingstory.polaris.parser.ParserProtos.MethodUsage;
import com.codingstory.polaris.parser.ParserProtos.TypeHandle;
import com.codingstory.polaris.parser.ParserProtos.TypeUsage;
import com.codingstory.polaris.parser.ParserProtos.Usage;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.TypeParameter;
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.codingstory.polaris.parser.ParserUtils.makeTypeName;
import static com.codingstory.polaris.parser.ParserUtils.nodeJumpTarget;
import static com.codingstory.polaris.parser.TypeUtils.getSimpleName;
import static com.codingstory.polaris.parser.TypeUtils.handleOf;
import static com.codingstory.polaris.parser.TypeUtils.usageOf;

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
        private final FileHandle file;
        private final SymbolTable symbolTable;
        private final List<Usage> usages = Lists.newArrayList();
        private final List<ClassType> discoveredClasses = Lists.newArrayList();
        private final String pkg;
        private final LinkedList<ClassType> typeStack = Lists.newLinkedList();
        private final LinkedList<Method> methodStack = Lists.newLinkedList();
        private final List<ImportDeclaration> imports = Lists.newArrayList();
        private final IdGenerator idGenerator;

        /** Symbol table for classTypes. */
        // TODO: symbol table for variables

        private ASTVisitor(String project,
                FileHandle file,
                SymbolTable symbolTable,
                IdGenerator idGenerator,
                String pkg) {
            this.project = Preconditions.checkNotNull(project);
            this.file = Preconditions.checkNotNull(file);
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
                String name = node.getName().toString();
                ClassTypeHandle clazz = symbolTable.resolveClassHandle(name);
                usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                        .setKind(TypeUsage.Kind.IMPORT)
                        .setType(TypeUtils.handleOf(clazz))
                        .build(), nodeJumpTarget(file, node.getName())));
                symbolTable.registerImportClass(clazz);
            }
            super.visit(node, arg);
        }

        @Override
        public void visit(final ClassOrInterfaceDeclaration node, final Object arg) {
            Preconditions.checkNotNull(node);
            List<ClassOrInterfaceType> superTypeAsts = ImmutableList.copyOf(Iterables.concat(
                    nullToEmptyList(node.getExtends()), nullToEmptyList(node.getImplements())));
            for (TypeParameter genericType : nullToEmptyList(node.getTypeParameters())) {
                usages.add(usageOf(TypeUsage.newBuilder()
                        .setKind(TypeUsage.Kind.GENERIC_TYPE_PARAMETER)
                        .setType(handleOf(symbolTable.resolveClassHandle(genericType.getName())))
                        .build(), nodeJumpTarget(file, genericType)));
            }
            processTypeDeclaration(node.getName(),
                    node.isInterface() ? ClassType.Kind.INTERFACE : ClassType.Kind.CLASS,
                    nodeJumpTarget(file, node.getNameExpr()),
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
            String fullName = makeTypeName(pkg, getOuterClassNames(), name);
            ClassType clazz = symbolTable.resolveClass(fullName);
            if (clazz == null) {
                throw new AssertionError(fullName + " should have been identified in 1st pass");
            }
            ClassType.Builder classBuilder = clazz.toBuilder();
            if (javaDoc != null) {
                classBuilder.setJavaDoc(javaDoc);
            }
            for (ClassOrInterfaceType superTypeAst : superTypeAsts) {
                String superTypeName = superTypeAst.toString();
                TypeHandle superType = symbolTable.resolveTypeHandle(superTypeName);
                usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                        .setType(superType)
                        .setKind(TypeUsage.Kind.SUPER_CLASS)
                        .build(), nodeJumpTarget(file, superTypeAst)));
                classBuilder.addSuperTypes(superType);
            }
            usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                    .setType(TypeUtils.handleOf(classBuilder.getHandle()))
                    .setKind(TypeUsage.Kind.TYPE_DECLARATION)
                    .build(), jumpTarget));
            clazz = classBuilder.build();
            symbolTable.enterScope();
            typeStack.push(clazz);
            visitChildren.run();
            discoveredClasses.add(typeStack.pop());
            symbolTable.leaveScope();
        }

        @Override
        public void visit(final AnnotationDeclaration node, final Object arg) {
            Preconditions.checkNotNull(node);
            processTypeDeclaration(node.getName(),
                    ClassType.Kind.ANNOTATION,
                    nodeJumpTarget(file, node.getNameExpr()),
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
                    nodeJumpTarget(file, node.getNameExpr()),
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
            processMethodDeclaration(node.getType(), node.getName(), nodeJumpTarget(file, node),
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
            processMethodDeclaration(null, "<init>", nodeJumpTarget(file, node),
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
            processMethodDeclaration(null, "<cinit>", nodeJumpTarget(file, node),
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
                returnType = TypeUtils.handleOf(PrimitiveTypes.VOID);
            } else {
                returnType = symbolTable.resolveTypeHandle(type.toString());
                usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                        .setKind(TypeUsage.Kind.METHOD_SIGNATURE)
                        .setType(returnType)
                        .build(), nodeJumpTarget(file, type)));
            }
            List<Method.Parameter> parameters = Lists.newArrayList();
            List<TypeHandle> parameterTypes = Lists.newArrayList();
            for (Parameter parameter : nullToEmptyList(methodParameters)) {
                TypeHandle parameterType = symbolTable.resolveTypeHandle(parameter.getType().toString());
                usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                        .setType(parameterType)
                        .setKind(TypeUsage.Kind.METHOD_SIGNATURE)
                        .build(), nodeJumpTarget(file, parameter.getType())));
                parameterTypes.add(parameterType);
                parameters.add(Method.Parameter.newBuilder()
                        .setType(parameterType)
                        .setName(parameter.getId().getName())
                        .build());
            }
            List<TypeHandle> exceptions = Lists.newArrayList();
            for (NameExpr throwExpr : nullToEmptyList(methodThrows)) {
                TypeHandle exceptionType = symbolTable.resolveTypeHandle(throwExpr.getName());
                usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                        .setType(exceptionType)
                        .setKind(TypeUsage.Kind.METHOD_SIGNATURE)
                        .build(), nodeJumpTarget(file, throwExpr)));
                exceptions.add(exceptionType);
            }
            String fullMemberName = currentTypeName() + "." + methodName;
            MethodHandle methodHandle = MethodHandle.newBuilder()
                    .setId(generateId())
                    .setName(fullMemberName)
                    .addAllParameters(parameterTypes)
                    .build();
            usages.add(TypeUtils.usageOf(MethodUsage.newBuilder()
                    .setMethod(methodHandle)
                    .setKind(MethodUsage.Kind.METHOD_DECLARATION)
                    .build(), jumpTarget));
            Method method = Method.newBuilder()
                    .setHandle(methodHandle)
                    .setReturnType(returnType)
                    .addAllParameters(parameters)
                    .addAllExceptions(exceptions)
                    .setJumpTarget(jumpTarget)
                    .build();
            addMethodToCurrentType(method);
            methodStack.push(method);
            visitChildren.run();
            methodStack.pop();
        }

        private void addMethodToCurrentType(Method method) {
            ClassType top = typeStack.pop();
            typeStack.push(top.toBuilder()
                    .addMethods(method)
                    .build());
        }

        @Override
        public void visit(japa.parser.ast.body.FieldDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            TypeHandle type = symbolTable.resolveTypeHandle(
                    dropGenericTypes(node.getType().toString()));
            usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                    .setType(type)
                    .setKind(TypeUsage.Kind.FIELD)
                    .build(), nodeJumpTarget(file, node.getType())));
            for (VariableDeclarator varDecl : node.getVariables()) {
                JumpTarget fieldTarget = nodeJumpTarget(file, varDecl.getId());
                String fullMemberName = currentTypeName() + "." + varDecl.getId().getName();
                FieldHandle fieldHandle = FieldHandle.newBuilder()
                        .setId(generateId())
                        .setName(fullMemberName)
                        .build();
                Field field = Field.newBuilder()
                        .setHandle(fieldHandle)
                        .setType(type)
                        .setJumpTarget(fieldTarget)
                        .build();
                addFieldToCurrentType(field);
                usages.add(TypeUtils.usageOf(FieldUsage.newBuilder()
                        .setField(field.getHandle())
                        .setKind(FieldUsage.Kind.FIELD_DECLARATION)
                        .build(), fieldTarget));
            }
            super.visit(node, arg);
        }

        /** Drops any generic types from a type name. For example, it returns "List" if passing "List<Integer>". */
        private String dropGenericTypes(String typeName) {
            int p = typeName.indexOf('<');
            if (p == -1) {
                return typeName;
            }
            return typeName.substring(0, p);
        }

        public void addFieldToCurrentType(Field field) {
            ClassType top = typeStack.pop();
            typeStack.push(top.toBuilder()
                    .addFields(field)
                    .build());
        }

        @Override
        public void visit(VariableDeclarationExpr node, Object arg) {
            Preconditions.checkNotNull(node);
            TypeHandle type = symbolTable.resolveTypeHandle(node.getType().toString());
            usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                    .setType(type)
                    .setKind(TypeUsage.Kind.LOCAL_VARIABLE)
                    .build(), nodeJumpTarget(file, node.getType())));
            super.visit(node, arg);
        }

        private ClassType currentType() {
            if (typeStack.isEmpty()) {
                return null;
            } else {
                return typeStack.getFirst();
            }
        }

        private String currentTypeName() {
            ClassType current = currentType();
            if (current == null) {
                return null;
            } else {
                return current.getHandle().getName();
            }
        }

        private Method currentMethod() {
            return methodStack.getLast();
        }

        public List<Usage> getUsages() { return usages; }

        public List<ClassType> getClassTypes() { return discoveredClasses; }

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

        public List<String> getOuterClassNames() {
            List<String> results = Lists.newArrayListWithCapacity(typeStack.size());
            for (ClassType clazz : typeStack) {
                results.add(getSimpleName(clazz.getHandle().getName()));
            }
            Collections.reverse(results);
            return results;
        }
    }

    private SecondPassProcessor() {}

    public static Result extract(String project,
            FileHandle file,
            InputStream in,
            SymbolTable symbolTable,
            IdGenerator idGenerator,
            String pkg) throws IOException {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(file);
        Preconditions.checkNotNull(in);
        Preconditions.checkNotNull(symbolTable);
        Preconditions.checkNotNull(idGenerator);
        symbolTable.enterCompilationUnit(pkg);
        ASTVisitor visitor = new ASTVisitor(project, file, symbolTable, idGenerator, pkg);
        ParserUtils.safeVisit(in, visitor);
        symbolTable.leaveCompilationUnit();
        return new Result(visitor.getClassTypes(), visitor.getUsages());
    }
}
