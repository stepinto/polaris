package com.codingstory.polaris.parser;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.IdUtils;
import com.codingstory.polaris.JumpTarget;
import com.codingstory.polaris.SkipCheckingExceptionWrapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
        private final TypeResolver typeResolver;
        private final List<Usage> usages = Lists.newArrayList();
        private final List<ClassType> discoveredTypes = Lists.newArrayList();
        private String packageName;
        private final LinkedList<ClassType> typeStack = Lists.newLinkedList();
        private final LinkedList<Method> methodStack = Lists.newLinkedList();
        private final List<ImportDeclaration> imports = Lists.newArrayList();
        private final IdGenerator idGenerator;

        /** Symbol table for classTypes. */
        private final TypeTable typeTable = new TypeTable();
        private final Map<String, TypeTable.Frame> typeTableFramesForPackage = Maps.newHashMap();
        // TODO: symbol table for variables

        private ASTVisitor(String project, long fileId,
                TypeResolver typeResolver, IdGenerator idGenerator) {
            this.project = Preconditions.checkNotNull(project);
            this.fileId = IdUtils.checkValid(fileId);
            this.typeResolver = new CascadeTypeResolver(ImmutableList.of(
                    PrimitiveTypeResolver.getInstance(),
                    typeTable.getTypeResolver(),
                    Preconditions.checkNotNull(typeResolver)));
            this.idGenerator = Preconditions.checkNotNull(idGenerator);
            typeTable.enterFrame();
            typeTableFramesForPackage.put("", typeTable.currentFrame());
        }

        @Override
        public void visit(japa.parser.ast.PackageDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            packageName = node.getName().toString();

            // Establish type-table frames for packages. Assume the current package is "a.b.c"
            // so there will be 3 frames: "a", "a.b" and "a.b.c".
            StringBuilder prefix = new StringBuilder();
            boolean first = true;
            for (String part : Splitter.on(".").split(packageName)) {
                if (!first) {
                    prefix.append(".");
                }
                prefix.append(part);
                typeTableFramesForPackage.put(prefix.toString(), typeTable.currentFrame());
                typeTable.enterFrame();
                first = false;
            }
            super.visit(node, arg);
        }

        @Override
        public void visit(ImportDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            if (node.isAsterisk()) {
                imports.add(node);
            } else {
                FullTypeName name = FullTypeName.of(node.getName().toString());
                TypeHandle type = typeResolver.resolve(name);
                if (type == null) {
                    type = new TypeHandle(Type.UNRESOLVED_TYPE_ID, name);
                }
                TypeTable.Frame typeTableFrame = typeTableFramesForPackage.get(name.getPackageName());
                if (typeTableFrame == null) {
                    typeTableFrame = typeTableFramesForPackage.get("");
                }
                typeTableFrame.put(type);
                usages.add(new TypeUsage(type, findTokenSpan(node), TypeUsage.Kind.IMPORT));
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
                    findTokenSpan(node),
                    superTypeAsts,
                    node.getJavaDoc() != null ? node.getJavaDoc().getContent() : null,
                    new Runnable() {
                        @Override
                        public void run() {
                            ASTVisitor.super.visit(node, arg);
                        }
                    });
        }

        private void processTypeDeclaration(String name, ClassType.Kind kind, Span span,
                List<ClassOrInterfaceType> superTypeAsts, String javaDoc, Runnable visitChildren) {
            List<TypeHandle> superTypes = Lists.newArrayList();
            for (ClassOrInterfaceType superTypeAst : superTypeAsts) {
                String superTypeName = superTypeAst.toString();
                TypeHandle superType = resolveType(superTypeName);
                superTypes.add(superType);
            }
            FullTypeName fullTypeName = FullTypeName.of(packageName, makeTypeName(name));
            TypeHandle handle = typeResolver.resolve(fullTypeName);
            if (handle == null) {
                throw new AssertionError(fullTypeName + " should have been identified in 1st pass");
            }
            ClassType type = new ClassType(
                    handle,
                    kind,
                    superTypes,
                    EnumSet.noneOf(Modifier.class), // TODO: set proper modifiers
                    Lists.<Field>newArrayList(),
                    Lists.<Method>newArrayList(),
                    javaDoc != null ? javaDoc.trim() : null,
                    new JumpTarget(fileId, span.getFrom()));
            usages.add(new TypeUsage(type.getHandle(), span, TypeUsage.Kind.TYPE_DECLARATION));
            for (ClassOrInterfaceType superTypeNode : superTypeAsts) {
                TypeHandle superType = resolveType(superTypeNode.getName());
                usages.add(new TypeUsage(superType, findTokenSpan(superTypeNode), TypeUsage.Kind.SUPER_CLASS));
            }
            discoveredTypes.add(type);
            typeTable.put(type.getHandle());
            typeTable.enterFrame();
            typeStack.push(type);
            visitChildren.run();
            typeStack.pop();
            typeTable.leaveFrame();
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
                    findTokenSpan(node),
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
                    findTokenSpan(node),
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
                japa.parser.ast.type.Type type,
                String methodName,
                Span span,
                List<Parameter> methodParameters,
                List<NameExpr> methodThrows,
                Runnable visitChildren) {
            TypeHandle returnType = type == null ? PrimitiveType.VOID.getHandle() : resolveType(type.toString());
            if (type != null) {
                usages.add(new TypeUsage(returnType, findTokenSpan(type), TypeUsage.Kind.METHOD_SIGNATURE));
            }
            List<Method.Parameter> parameters = Lists.newArrayList();
            List<TypeHandle> parameterTypes = Lists.newArrayList();
            for (Parameter parameter : nullToEmptyList(methodParameters)) {
                TypeHandle parameterType = resolveType(parameter.getType().toString());
                usages.add(new TypeUsage(parameterType, findTokenSpan(parameter.getType()),
                        TypeUsage.Kind.METHOD_SIGNATURE));
                parameterTypes.add(parameterType);
                parameters.add(new Method.Parameter(parameterType, parameter.getId().getName()));
            }
            List<TypeHandle> exceptions = Lists.newArrayList();
            for (NameExpr throwExpr : nullToEmptyList(methodThrows)) {
                TypeHandle exceptionType = resolveType(throwExpr.getName());
                usages.add(new TypeUsage(exceptionType,
                        findTokenSpan(throwExpr), TypeUsage.Kind.METHOD_SIGNATURE));
                exceptions.add(exceptionType);
            }
            FullMemberName fullMemberName = FullMemberName.of(currentType().getName(), methodName);
            Method method = new Method(
                    new MethodHandle(generateId(), fullMemberName, parameterTypes),
                    returnType,
                    parameters,
                    exceptions,
                    EnumSet.noneOf(Modifier.class),
                    new JumpTarget(fileId, span.getFrom()));
            usages.add(new MethodUsage(method.getHandle(), span, MethodUsage.Kind.METHOD_DECLARATION));
            currentType().getMethods().add(method);
            methodStack.push(method);
            visitChildren.run();
            methodStack.pop();
        }

        @Override
        public void visit(japa.parser.ast.body.FieldDeclaration node, Object arg) {
            Preconditions.checkNotNull(node);
            TypeHandle type = resolveType(node.getType().toString());
            Span typeSpan = findTokenSpan(node.getType());
            usages.add(new TypeUsage(type, typeSpan, TypeUsage.Kind.FIELD));
            for (VariableDeclarator varDecl : node.getVariables()) {
                Span fieldSpan = findTokenSpan(varDecl.getId());
                FullMemberName fullMemberName = FullMemberName.of(currentType().getName(), varDecl.getId().getName());
                Field field = new Field(
                        new FieldHandle(generateId(), fullMemberName),
                        type,
                        EnumSet.noneOf(Modifier.class),
                        new JumpTarget(fileId, fieldSpan.getFrom()));
                currentType().getFields().add(field);
                usages.add(new FieldUsage(field.getHandle(), fieldSpan, FieldUsage.Kind.FIELD_DECLARATION));
            }
            super.visit(node, arg);
        }

        @Override
        public void visit(VariableDeclarationExpr node, Object arg) {
            Preconditions.checkNotNull(node);
            TypeHandle type = resolveType(node.getType().toString());
            usages.add(new TypeUsage(type, findTokenSpan(node.getType()), TypeUsage.Kind.LOCAL_VARIABLE));
            super.visit(node, arg);
        }

        private ClassType currentType() {
            return typeStack.getFirst();
        }

        private TypeHandle resolveType(String name) {
            FullTypeName fullName = FullTypeName.of(name);
            TypeHandle type = typeResolver.resolve(fullName);
            if (type != null) {
                return type;
            }
            if (fullName.hasPackageName()) {
                return new TypeHandle(Type.UNRESOLVED_TYPE_ID, fullName);
            }
            // Assume the type is "Type", the package is "mypkg" and "import pkg1.*" and "import pkg2.*".
            // We will generate the following candidates:
            //   mypkg.Type
            //   Type
            //   pkg1.Type
            //   pkg2.Type
            List<FullTypeName> candidates = Lists.newArrayList();
            candidates.add(FullTypeName.of(packageName, name));
            candidates.add(FullTypeName.of(null, name));
            for (ImportDeclaration imp : imports) {
                if (imp.isAsterisk()) {
                    String packageName = imp.getName().getName();
                    candidates.add(FullTypeName.of(packageName, name));
                }
            }
            for (FullTypeName candidate : candidates) {
                type = typeResolver.resolve(candidate);
                if (type != null) {
                    return type;
                }
            }
            return new TypeHandle(Type.UNRESOLVED_TYPE_ID, FullTypeName.of(name));
        }

        private Span findTokenSpan(Node node) {
            Preconditions.checkNotNull(node);
            Position from = new Position(node.getBeginLine() - 1, node.getBeginColumn() - 1);
            Position to = new Position(node.getEndLine() - 1, node.getEndColumn());
            return new Span(from, to);
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
            TypeResolver externalTypeResolver,
            IdGenerator idGenerator) throws IOException {
        Preconditions.checkNotNull(project);
        IdUtils.checkValid(fileId);
        Preconditions.checkNotNull(in);
        Preconditions.checkNotNull(externalTypeResolver);
        Preconditions.checkNotNull(idGenerator);
        ASTVisitor visitor = new ASTVisitor(project, fileId, externalTypeResolver, idGenerator);
        ParserUtils.safeVisit(in, visitor);
        return new Result(visitor.getClassTypes(), visitor.getUsages());
    }
}
