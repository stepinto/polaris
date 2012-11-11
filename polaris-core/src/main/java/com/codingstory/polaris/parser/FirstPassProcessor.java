package com.codingstory.polaris.parser;

import com.codingstory.polaris.IdGenerator;
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
import java.util.LinkedList;
import java.util.List;

/** Extracts full type names with assigned type ids. */
public class FirstPassProcessor {
    private static class FirstPassVisitor extends VoidVisitorAdapter<Void> {
        private String packageName = "";
        private final List<TypeHandle> types = Lists.newArrayList();
        private final IdGenerator idGenerator;
        private final LinkedList<TypeHandle> typeStack = Lists.newLinkedList();

        private FirstPassVisitor(IdGenerator idGenerator) {
            this.idGenerator = Preconditions.checkNotNull(idGenerator);
        }

        @Override
        public void visit(PackageDeclaration ast, Void arg) {
            packageName = ast.getName().toString();
            super.visit(ast, arg);
        }

        @Override
        public void visit(AnnotationDeclaration ast, Void arg) {
            processTypeAndPushStack(ast.getName());
            super.visit(ast, arg);
            typeStack.pop();
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration ast, Void arg) {
            processTypeAndPushStack(ast.getName());
            super.visit(ast, arg);
            typeStack.pop();
        }

        @Override
        public void visit(EnumDeclaration ast, Void arg) {
            processTypeAndPushStack(ast.getName());
            super.visit(ast, arg);
            typeStack.pop();
        }

        private void processTypeAndPushStack(String name) {
            try {
                FullTypeName typeName;
                if (typeStack.isEmpty()) {
                    typeName = FullTypeName.of(packageName, name);
                } else {
                    typeName = FullTypeName.of(packageName, typeStack.getFirst().getName().getTypeName() + "$" + name);
                }
                TypeHandle type = new TypeHandle(idGenerator.next(), typeName);
                typeStack.push(type);
                types.add(type);
            } catch (IOException e) {
                throw new SkipCheckingExceptionWrapper(e);
            }
        }

        public List<TypeHandle> getTypes() {
            return types;
        }
    }

    public static List<TypeHandle> process(InputStream in, IdGenerator idGenerator) throws IOException {
        Preconditions.checkNotNull(in);
        Preconditions.checkNotNull(idGenerator);
        FirstPassVisitor visitor = new FirstPassVisitor(idGenerator);
        ParserUtils.safeVisit(in, visitor);
        return visitor.getTypes();
    }
}
