package com.codingstory.polaris.parser;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.ClassTypeHandle;
import com.codingstory.polaris.parser.ParserProtos.FileHandle;
import com.codingstory.polaris.parser.ParserProtos.JumpTarget;
import com.codingstory.polaris.parser.ParserProtos.Method;
import com.codingstory.polaris.parser.ParserProtos.MethodUsage;
import com.codingstory.polaris.parser.ParserProtos.Span;
import com.codingstory.polaris.parser.ParserProtos.Type;
import com.codingstory.polaris.parser.ParserProtos.TypeHandle;
import com.codingstory.polaris.parser.ParserProtos.TypeKind;
import com.codingstory.polaris.parser.ParserProtos.TypeUsage;
import com.codingstory.polaris.parser.ParserProtos.Usage;
import com.codingstory.polaris.parser.ParserProtos.Variable;
import com.codingstory.polaris.parser.ParserProtos.VariableHandle;
import com.codingstory.polaris.parser.ParserProtos.VariableUsage;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.AnnotationDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.expr.ThisExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.List;

import static com.codingstory.polaris.CollectionUtils.nullToEmptyCollection;
import static com.codingstory.polaris.parser.ParserUtils.dropGenericTypes;
import static com.codingstory.polaris.parser.ParserUtils.nodeJumpTarget;
import static com.codingstory.polaris.parser.ParserUtils.nodeSpan;
import static com.codingstory.polaris.parser.TypeUtils.createTypeUsage;
import static com.codingstory.polaris.parser.TypeUtils.handleOf;
import static com.codingstory.polaris.parser.TypeUtils.snippetLine;
import static com.codingstory.polaris.parser.TypeUtils.unresolvedTypeHandleOf;
import static com.codingstory.polaris.parser.TypeUtils.usageOf;

/** Generates cross references for local variable declarationmethod calls. */
public class ThirdPassProcessor {

    private static final Log LOG = LogFactory.getLog(ThirdPassProcessor.class);

    private static class ThirdPassVisitor extends VoidVisitorAdapter<Void> {
        private final FileHandle file;
        private final SymbolTable symbolTable;
        private final List<Usage> usages = Lists.newArrayList();
        private final String[] lines;
        private final IdGenerator idGenerator;

        private ThirdPassVisitor(FileHandle file, String source, SymbolTable symbolTable, IdGenerator idGenerator) {
            this.file = Preconditions.checkNotNull(file);
            this.symbolTable = Preconditions.checkNotNull(symbolTable);
            this.lines = source.split("\n");
            this.idGenerator = Preconditions.checkNotNull(idGenerator);
        }

        @Override
        public void visit(ImportDeclaration node, Void arg) {
            Preconditions.checkNotNull(node);
            if (node.isAsterisk()) {
                symbolTable.registerImportPackage(node.getName().toString());
            } else {
                String name = node.getName().toString();
                ClassTypeHandle clazz = symbolTable.resolveClassHandle(name);
                symbolTable.registerImportClass(clazz);
            }
            super.visit(node, arg);
        }

        @Override
        public void visit(final ClassOrInterfaceDeclaration node, final Void arg) {
            Preconditions.checkNotNull(node);
            processTypeDeclaration(node, new Runnable() {
                @Override
                public void run() {
                    ThirdPassVisitor.super.visit(node, arg);
                }
            });
        }

        @Override
        public void visit(final EnumDeclaration node, final Void arg) {
            Preconditions.checkNotNull(node);
            processTypeDeclaration(node, new Runnable() {
                @Override
                public void run() {
                    ThirdPassVisitor.super.visit(node, arg);
                }
            });
        }

        @Override
        public void visit(final AnnotationDeclaration node, final Void arg) {
            Preconditions.checkNotNull(node);
            processTypeDeclaration(node, new Runnable() {
                @Override
                public void run() {
                    ThirdPassVisitor.super.visit(node, arg);
                }
            });
        }

        private void processTypeDeclaration(TypeDeclaration typeDeclaration, Runnable visitChildren) {
            ClassType clazz = symbolTable.getClassTypeByLocation(file, nodeSpan(typeDeclaration.getNameExpr()));
            if (clazz == null) {
                LOG.error("nodeSpan(typeDeclaration.getNameExpr()) = " + nodeSpan(typeDeclaration.getNameExpr()));
                throw new AssertionError("The class should have been discovered by previous passes "
                        + typeDeclaration.getName());
            }
            symbolTable.enterClassScope(clazz);
            visitChildren.run();
            symbolTable.leaveClassScope();
        }

        @Override
        public void visit(MethodDeclaration node, Void arg) {
            Preconditions.checkNotNull(node);
            Method method = findMethodBySpan(symbolTable.currentClass(), nodeSpan(node.getNameExpr()));
            if (method == null) {
                throw new AssertionError("This method should have been discovered by previous passes: "
                        + node.getName());
            }
            symbolTable.enterMethodScope(method);
            super.visit(node, arg);    //To change body of overridden methods use File | Settings | File Templates.
            symbolTable.leaveMethodScope();
        }

        private Method findMethodBySpan(ClassType clazz, Span span) {
            for (Method method : clazz.getMethodsList()) {
                if (Objects.equal(method.getJumpTarget().getSpan(), span)) {
                    return method;
                }
            }
            return null;
        }

        @Override
        public void visit(VariableDeclarationExpr node, Void arg) {
            Preconditions.checkNotNull(node);
            String className = dropGenericTypes(node.getType().toString());
            Type type = symbolTable.resolveType(className);
            TypeHandle typeHandle = (type == null ? unresolvedTypeHandleOf(className) : handleOf(type));
            JumpTarget typeJumpTarget = nodeJumpTarget(file, node.getType());
            usages.add(createTypeUsage(
                    type, className, TypeUsage.Kind.LOCAL_VARIABLE, typeJumpTarget, snippetLine(lines, typeJumpTarget)));
            for (VariableDeclarator decl : node.getVars()) {
                JumpTarget variableJumpTarget = nodeJumpTarget(file, decl.getId());
                String variableName = decl.getId().getName();
                VariableHandle variableHandle = VariableHandle.newBuilder()
                        .setId(idGenerator.next())
                        .setName(variableName)
                        .build();
                Variable variable = Variable.newBuilder()
                        .setHandle(variableHandle)
                        .setJumpTarget(variableJumpTarget)
                        .setKind(Variable.Kind.LOCAL_VARIABLE)
                        .setType(typeHandle)
                        .build();
                symbolTable.registerVariable(variable);
                usages.add(TypeUtils.usageOf(VariableUsage.newBuilder()
                        .setVariable(variableHandle)
                        .setKind(VariableUsage.Kind.DECLARATION)
                        .build(), variableJumpTarget, variableJumpTarget, snippetLine(lines, variableJumpTarget)));
            }
            super.visit(node, arg);
        }

        @Override
        public void visit(MethodCallExpr node, Void arg) {
            Preconditions.checkNotNull(node);
            super.visit(node, arg); // visit scope first to determine its type

            Expression scope = node.getScope();
            TypeHandle type = null;
            if (scope == null) {
                // implict "this"
                // TODO: calling static method
                type = handleOf(symbolTable.currentClass().getHandle());
            } else {
                type = symbolTable.getExpressionType(scope);
            }
            processMethodCall(
                    type,
                    node.getName(),
                    nullToEmptyCollection(node.getArgs()).size(),
                    MethodUsage.Kind.METHOD_CALL,
                    nodeJumpTarget(file, node.getNameExpr()));
        }

        @Override
        public void visit(ObjectCreationExpr node, Void arg) {
            Preconditions.checkNotNull(node);
            TypeHandle type = symbolTable.resolveTypeHandle(node.getType().getName());
            processMethodCall(
                    type,
                    "<init>",
                    nullToEmptyCollection(node.getArgs()).size(),
                    MethodUsage.Kind.INSTANCE_CREATION,
                    nodeJumpTarget(file, node.getType()));
            symbolTable.registerExpressionType(node, type);
            super.visit(node, arg);
        }


        @Override
        public void visit(ThisExpr node, Void arg) {
            Preconditions.checkNotNull(node);
            // TODO: handle OutterClassName.this
            TypeHandle type = handleOf(symbolTable.currentClass().getHandle());
            symbolTable.registerExpressionType(node, type);
            super.visit(node, arg);
        }

        private void processMethodCall(TypeHandle type, String methodName, int argc,
                MethodUsage.Kind kind, JumpTarget jumpTarget) {
            if (type != null && type.getKind() == TypeKind.CLASS && type.getClazz().getResolved()) {
                ClassType clazz = symbolTable.getClassByHandle(type.getClazz());
                if (clazz != null) {
                    Method method = findMethodInClass(clazz, methodName, argc);
                    if (method != null) {
                        String snippet = snippetLine(lines, jumpTarget);
                        usages.add(usageOf(MethodUsage.newBuilder()
                                .setKind(kind)
                                .setMethod(method.getHandle())
                                .build(), jumpTarget, method.getJumpTarget(), snippet));
                    }
                }
            }
        }

        private Method findMethodInClass(ClassType clazz, String methodName, int argumentCount) {
            List<Method> results = Lists.newArrayList();
            String prefix = clazz.getHandle().getName() + ".";
            for (Method method : clazz.getMethodsList()) {
                if (Objects.equal(method.getHandle().getName(), prefix + methodName) &&
                        method.getParametersCount() == argumentCount) {
                    results.add(method);
                }
            }
            if (results.size() == 1) {
                return Iterables.getOnlyElement(results);
            } else {
                return null; // TODO: Support overload.
            }
        }

        @Override
        public void visit(NameExpr node, Void arg) {
            super.visit(node, arg);
            Variable variable;
            TypeHandle type;
            if ((variable = symbolTable.getVariable(node.getName())) != null) {
                JumpTarget jumpTarget = nodeJumpTarget(file, node);
                usages.add(usageOf(VariableUsage.newBuilder()
                        .setKind(VariableUsage.Kind.ACCESS)
                        .setVariable(variable.getHandle())
                        .build(), jumpTarget, variable.getJumpTarget(), snippetLine(lines, jumpTarget)));
                symbolTable.registerExpressionType(node, variable.getType());
            } else if ((type = symbolTable.resolveTypeHandle(node.getName())) != null) {
                symbolTable.registerExpressionType(node, type); // e.g. calling static method of class
            }
        }

        public List<Usage> getUsages() {
            return usages;
        }
    }

    public static List<Usage> extract(
            FileHandle file,
            String source,
            SymbolTable symbolTable,
            String pkg,
            IdGenerator idGenerator) throws IOException {
        Preconditions.checkNotNull(file);
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(pkg);
        Preconditions.checkNotNull(idGenerator);
        symbolTable.enterCompilationUnit(pkg);
        ThirdPassVisitor visitor = new ThirdPassVisitor(file, source, symbolTable, idGenerator);
        ParserUtils.safeVisit(source, visitor);
        symbolTable.leaveCompilationUnit();
        return visitor.getUsages();
    }
}
