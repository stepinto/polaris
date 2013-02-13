package com.codingstory.polaris.parser;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.SkipCheckingExceptionWrapper;
import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.ClassTypeHandle;
import com.codingstory.polaris.parser.ParserProtos.FileHandle;
import com.codingstory.polaris.parser.ParserProtos.JumpTarget;
import com.codingstory.polaris.parser.ParserProtos.Span;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.body.AnnotationDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import static com.codingstory.polaris.parser.ParserUtils.makeTypeName;
import static com.codingstory.polaris.parser.ParserUtils.nodeSpan;

/** Extracts full type names with assigned type ids. */
public class FirstPassProcessor {

    private static final Log LOG = LogFactory.getLog(FirstPassProcessor.class);

    public static class Result {
        private final String pkg;
        private final List<ClassType> discoveredClasses;

        public Result(String pkg, List<ClassType> discoveredClasses) {
            this.pkg = pkg;
            this.discoveredClasses = discoveredClasses;
        }

        public String getPackage() {
            return pkg;
        }

        public List<ClassType> getDiscoveredClasses() {
            return discoveredClasses;
        }
    }

    private static class FirstPassVisitor extends VoidVisitorAdapter<Void> {
        private String pkg = "";
        private final FileHandle file;
        private final IdGenerator idGenerator;
        private final LinkedList<String> typeStack = Lists.newLinkedList();
        private final SymbolTable symbolTable;
        private final List<ClassType> discoveredClasses = Lists.newArrayList();

        private FirstPassVisitor(FileHandle file, IdGenerator idGenerator, SymbolTable symbolTable) {
            this.file = Preconditions.checkNotNull(file);
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
            typeStack.removeLast();
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration ast, Void arg) {
            processTypeAndPushStack(
                    ast.getName(),
                    ast.isInterface() ? ClassType.Kind.INTERFACE : ClassType.Kind.CLASS,
                    nodeSpan(ast));
            super.visit(ast, arg);
            typeStack.removeLast();
        }

        @Override
        public void visit(EnumDeclaration ast, Void arg) {
            processTypeAndPushStack(ast.getName(), ClassType.Kind.ENUM, nodeSpan(ast));
            super.visit(ast, arg);
            typeStack.removeLast();
        }

        private void processTypeAndPushStack(String simpleName, ClassType.Kind kind, Span span) {
            try {
                String fullName = makeTypeName(pkg, typeStack, simpleName);
                ClassTypeHandle handle = ClassTypeHandle.newBuilder()
                        .setId(idGenerator.next())
                        .setName(fullName)
                        .setResolved(true)
                        .build();
                LOG.debug("Allocated type handle: " + handle);
                JumpTarget jumpTarget = JumpTarget.newBuilder()
                        .setFile(file)
                        .setSpan(span)
                        .build();
                ClassType clazz = ClassType.newBuilder()
                        .setHandle(handle)
                        .setKind(kind)
                        .setJumpTarget(jumpTarget)
                        .build();
                typeStack.add(simpleName);
                symbolTable.registerClassType(clazz);
                discoveredClasses.add(clazz);
            } catch (IOException e) {
                throw new SkipCheckingExceptionWrapper(e);
            }
        }

        public Result getResult() {
            return new Result(pkg, discoveredClasses);
        }
    }

    public static Result process(FileHandle file, InputStream in, IdGenerator idGenerator, SymbolTable symbolTable) throws IOException {
        FirstPassVisitor visitor = new FirstPassVisitor(
                Preconditions.checkNotNull(file),
                Preconditions.checkNotNull(idGenerator),
                Preconditions.checkNotNull(symbolTable));
        ParserUtils.safeVisit(in, visitor);
        return visitor.getResult();
    }
}
