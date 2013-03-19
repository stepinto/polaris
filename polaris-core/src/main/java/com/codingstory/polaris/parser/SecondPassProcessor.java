package com.codingstory.polaris.parser;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.ClassTypeHandle;
import com.codingstory.polaris.parser.ParserProtos.Variable;
import com.codingstory.polaris.parser.ParserProtos.VariableHandle;
import com.codingstory.polaris.parser.ParserProtos.VariableUsage;
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
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.codingstory.polaris.CollectionUtils.nullToEmptyList;
import static com.codingstory.polaris.parser.ParserUtils.dropGenericTypes;
import static com.codingstory.polaris.parser.ParserUtils.makeTypeName;
import static com.codingstory.polaris.parser.ParserUtils.nodeJumpTarget;
import static com.codingstory.polaris.parser.TypeUtils.getSimpleName;
import static com.codingstory.polaris.parser.TypeUtils.handleOf;
import static com.codingstory.polaris.parser.TypeUtils.snippetLine;
import static com.codingstory.polaris.parser.TypeUtils.usageOf;

/** Extracts class members and generate cross references for field types and method signatures. */
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
        private final String[] lines;

        /** Symbol table for classTypes. */
        // TODO: symbol table for variables

        private ASTVisitor(String project,
                FileHandle file,
                String source,
                SymbolTable symbolTable,
                IdGenerator idGenerator,
                String pkg) {
            this.project = Preconditions.checkNotNull(project);
            this.file = Preconditions.checkNotNull(file);
            this.symbolTable = Preconditions.checkNotNull(symbolTable);
            this.idGenerator = Preconditions.checkNotNull(idGenerator);
            this.pkg = Preconditions.checkNotNull(pkg);
            this.lines = source.split("\n");
        }

        @Override
        public void visit(ImportDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            if (node.isAsterisk()) {
                symbolTable.registerImportPackage(node.getName().toString());
            } else {
                String name = node.getName().toString();
                ClassTypeHandle clazz = symbolTable.resolveClassHandle(name);
                JumpTarget jumpTarget = nodeJumpTarget(file, node.getName());
                usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                        .setKind(TypeUsage.Kind.IMPORT)
                        .setType(TypeUtils.handleOf(clazz))
                        .build(), jumpTarget, snippetLine(lines, jumpTarget)));
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
                JumpTarget jumpTarget = nodeJumpTarget(file, genericType);
                usages.add(usageOf(TypeUsage.newBuilder()
                        .setKind(TypeUsage.Kind.GENERIC_TYPE_PARAMETER)
                        .setType(handleOf(symbolTable.resolveClassHandle(genericType.getName())))
                        .build(), jumpTarget, snippetLine(lines, jumpTarget)));
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
            ClassType clazz = symbolTable.getClassTypeByLocation(file, jumpTarget.getSpan());
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
                JumpTarget superJumpTarget = nodeJumpTarget(file, superTypeAst);
                usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                        .setType(superType)
                        .setKind(TypeUsage.Kind.SUPER_CLASS)
                        .build(), superJumpTarget, snippetLine(lines, superJumpTarget)));
                classBuilder.addSuperTypes(superType);
            }
            usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                    .setType(TypeUtils.handleOf(classBuilder.getHandle()))
                    .setKind(TypeUsage.Kind.TYPE_DECLARATION)
                    .build(), jumpTarget, snippetLine(lines, jumpTarget)));
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
            processMethodDeclaration(node.getType(), node.getName(), nodeJumpTarget(file, node.getNameExpr()),
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
            processMethodDeclaration(null, "<init>", nodeJumpTarget(file, node.getNameExpr()),
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
                JumpTarget returnJumpTarget = nodeJumpTarget(file, type);
                usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                        .setKind(TypeUsage.Kind.METHOD_SIGNATURE)
                        .setType(returnType)
                        .build(), returnJumpTarget, snippetLine(lines, returnJumpTarget)));
            }
            List<Variable> parameters = Lists.newArrayList();
            List<TypeHandle> parameterTypes = Lists.newArrayList();
            for (Parameter parameter : nullToEmptyList(methodParameters)) {
                TypeHandle parameterType = symbolTable.resolveTypeHandle(parameter.getType().toString());
                JumpTarget parameterTypeJumpTarget = nodeJumpTarget(file, parameter.getType());
                usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                        .setType(parameterType)
                        .setKind(TypeUsage.Kind.METHOD_SIGNATURE)
                        .build(), parameterTypeJumpTarget, snippetLine(lines, parameterTypeJumpTarget)));
                parameterTypes.add(parameterType);
                VariableHandle handle = VariableHandle.newBuilder()
                        .setId(idGenerator.next())
                        .setName(parameter.getId().getName())
                        .build();
                parameters.add(Variable.newBuilder()
                        .setType(parameterType)
                        .setHandle(handle)
                        .setKind(Variable.Kind.PARAMETER)
                        .build());
                JumpTarget parameterJumpTarget = nodeJumpTarget(file, parameter.getId());
                usages.add(TypeUtils.usageOf(VariableUsage.newBuilder()
                        .setKind(VariableUsage.Kind.DECLARATION)
                        .setVariable(handle)
                        .build(), parameterJumpTarget, snippetLine(lines, parameterJumpTarget)));
            }
            List<TypeHandle> exceptions = Lists.newArrayList();
            for (NameExpr throwExpr : nullToEmptyList(methodThrows)) {
                TypeHandle exceptionType = symbolTable.resolveTypeHandle(throwExpr.getName());
                JumpTarget throwJumpTarget = nodeJumpTarget(file, throwExpr);
                usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                        .setType(exceptionType)
                        .setKind(TypeUsage.Kind.METHOD_SIGNATURE)
                        .build(), throwJumpTarget, snippetLine(lines, throwJumpTarget)));
                exceptions.add(exceptionType);
            }
            String fullMemberName = currentTypeName() + "." + methodName;
            MethodHandle methodHandle = MethodHandle.newBuilder()
                    .setId(idGenerator.next())
                    .setName(fullMemberName)
                    .addAllParameters(parameterTypes)
                    .build();
            usages.add(TypeUtils.usageOf(MethodUsage.newBuilder()
                    .setMethod(methodHandle)
                    .setKind(MethodUsage.Kind.METHOD_DECLARATION)
                    .build(), jumpTarget, snippetLine(lines, jumpTarget)));
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
            JumpTarget jumpTarget = nodeJumpTarget(file, node.getType());
            usages.add(TypeUtils.usageOf(TypeUsage.newBuilder()
                    .setType(type)
                    .setKind(TypeUsage.Kind.FIELD)
                    .build(), jumpTarget, snippetLine(lines, jumpTarget)));
            for (VariableDeclarator varDecl : node.getVariables()) {
                JumpTarget fieldTarget = nodeJumpTarget(file, varDecl.getId());
                String fullMemberName = currentTypeName() + "." + varDecl.getId().getName();
                VariableHandle fieldHandle = VariableHandle.newBuilder()
                        .setId(idGenerator.next())
                        .setName(fullMemberName)
                        .build();
                Variable field = Variable.newBuilder()
                        .setHandle(fieldHandle)
                        .setType(type)
                        .setKind(Variable.Kind.FIELD)
                        .setJumpTarget(fieldTarget)
                        .build();
                addFieldToCurrentType(field);
                usages.add(TypeUtils.usageOf(VariableUsage.newBuilder()
                        .setVariable(field.getHandle())
                        .setKind(VariableUsage.Kind.DECLARATION)
                        .build(), fieldTarget, snippetLine(lines, fieldTarget)));
            }
            super.visit(node, arg);
        }

        public void addFieldToCurrentType(Variable field) {
            ClassType top = typeStack.pop();
            typeStack.push(top.toBuilder()
                    .addFields(field)
                    .build());
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
            String source,
            SymbolTable symbolTable,
            IdGenerator idGenerator,
            String pkg) throws IOException {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(file);
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(symbolTable);
        Preconditions.checkNotNull(idGenerator);
        symbolTable.enterCompilationUnit(pkg);
        ASTVisitor visitor = new ASTVisitor(project, file, source, symbolTable, idGenerator, pkg);
        ParserUtils.safeVisit(source, visitor);
        symbolTable.leaveCompilationUnit();
        return new Result(visitor.getClassTypes(), visitor.getUsages());
    }
}
