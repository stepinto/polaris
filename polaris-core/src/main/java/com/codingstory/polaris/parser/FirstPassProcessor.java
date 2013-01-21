package com.codingstory.polaris.parser;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.IdUtils;
import com.codingstory.polaris.JumpTarget;
import com.codingstory.polaris.SkipCheckingExceptionWrapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.body.AnnotationDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.LinkedList;

import static com.codingstory.polaris.parser.ParserUtils.nodeSpan;

/** Extracts full type names with assigned type ids. */
public class FirstPassProcessor {
    public static class Result {
        private final String pkg;

        public Result(String pkg) {
            this.pkg = pkg;
        }

        public String getPackage() {
            return pkg;
        }
    }

    private static class FirstPassVisitor extends VoidVisitorAdapter<Void> {
        private String pkg = "";
        private long fileId;
        private final IdGenerator idGenerator;
        private final LinkedList<TypeHandle> typeStack = Lists.newLinkedList();
        private final SymbolTable symbolTable;

        private FirstPassVisitor(long fileId, IdGenerator idGenerator, SymbolTable symbolTable) {
            this.fileId = IdUtils.checkValid(fileId);
            this.idGenerator = Preconditions.checkNotNull(idGenerator);
            this.symbolTable = Preconditions.checkNotNull(symbolTable);
        }

        @Override
        public void visit(PackageDeclaration ast, Void arg) {
            pkg = ast.getName().toString();
            super.visit(ast, arg);
        }

        @Override
        public void visit(AnnotationDeclaration ast, Void arg) {
            processTypeAndPushStack(ast.getName(), ClassType.Kind.ANNOTATION, nodeSpan(ast));
            super.visit(ast, arg);
            typeStack.pop();
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration ast, Void arg) {
            processTypeAndPushStack(
                    ast.getName(),
                    ast.isInterface() ? ClassType.Kind.INTERFACE : ClassType.Kind.CLASS,
                    nodeSpan(ast));
            super.visit(ast, arg);
            typeStack.pop();
        }

        @Override
        public void visit(EnumDeclaration ast, Void arg) {
            processTypeAndPushStack(ast.getName(), ClassType.Kind.ENUM, nodeSpan(ast));
            super.visit(ast, arg);
            typeStack.pop();
        }

        private void processTypeAndPushStack(String name, ClassType.Kind kind, Span span) {
            try {
                FullTypeName typeName;
                if (typeStack.isEmpty()) {
                    typeName = FullTypeName.of(pkg, name);
                } else {
                    typeName = FullTypeName.of(pkg, typeStack.getFirst().getName().getTypeName() + "$" + name);
                }
                TypeHandle handle = new TypeHandle(idGenerator.next(), typeName);
                ClassType clazz = new ClassType(
                        handle, kind, EnumSet.noneOf(Modifier.class),  null, new JumpTarget(fileId, span));
                typeStack.push(handle);
                symbolTable.registerClassType(clazz);
            } catch (IOException e) {
                throw new SkipCheckingExceptionWrapper(e);
            }
        }

        public Result getResult() {
            return new Result(pkg);
        }
    }

    public static Result process(long fileId, InputStream in, IdGenerator idGenerator, SymbolTable symbolTable) throws IOException {
        FirstPassVisitor visitor = new FirstPassVisitor(
                IdUtils.checkValid(fileId),
                Preconditions.checkNotNull(idGenerator),
                Preconditions.checkNotNull(symbolTable));
        ParserUtils.safeVisit(in, visitor);
        return visitor.getResult();
    }
}
