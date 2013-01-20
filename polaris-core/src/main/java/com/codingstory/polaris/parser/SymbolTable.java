package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.LinkedList;
import java.util.Map;

/**
 * Symbol table for types.
 */
public class SymbolTable {

    public static enum EntryKind {
        CLASS_PLACEHOLDER,
        CLASS,
        LOCAL_CLASS,
        FIELD,
        // TODO: LOCAL_VARIABLE
    }

    public static class Entry {
        private EntryKind kind;
        private ClassType classType;
        private Field field;

        public EntryKind getKind() {
            return kind;
        }

        public ClassType asClassType() {
            Preconditions.checkNotNull(kind == EntryKind.CLASS || kind == EntryKind.LOCAL_CLASS);
            return classType;
        }

        public Field asField() {
            Preconditions.checkNotNull(kind == EntryKind.FIELD);
            return field;
        }
    }

    private final Map<FullTypeName, ClassType> fullyNamedTypes = Maps.newHashMap();
    private String packageScope;
    private Map<String, FullTypeName> imports;

    public static class Frame {
        private final Map<String, ClassType> shortlyNamedTypes = Maps.newHashMap();
        private final Map<String, FullTypeName> imports = Maps.newHashMap();

        public void registerClassType(ClassType classType) {
            Preconditions.checkNotNull(classType);
            FullTypeName typeName = classType.getName();
            shortlyNamedTypes.put(typeName.getTypeName(), classType);
        }

        public ClassType lookUpClassBySimpleName(String name) {
            return shortlyNamedTypes.get(name);
        }

        public Iterable<ClassType> getClasses() {
            return shortlyNamedTypes.values();
        }
    }

    private final LinkedList<Frame> frames = Lists.newLinkedList();

    public SymbolTable() {
        frames.add(new Frame());
    }

    /**
     * Resolves primitive type or class.
     *
     * @return the resolved type on success, otherwise {@code null} */
    public Type resolveType(FullTypeName name) {
        Preconditions.checkNotNull(name);
        Type primitiveType = TypeResolver.resolvePrimitiveType(name);
        if (primitiveType != null) {
            return primitiveType;
        }
        return lookUpClassType(name);
    }

    /**
     * Resolves primitive or class, returning its handle.
     *
     * @return the hanlde of the resolved type on success, otherwise an unresolved type
     */
    public TypeHandle resolveTypeHandle(FullTypeName name) {
        Type type = resolveType(name);
        if (type != null) {
            return type.getHandle();
        }
        FullTypeName fullName = resolveImportAlias(name.getTypeName());
        return TypeHandle.createUnresolved(fullName == null ? name : fullName);
    }

    public ClassType lookUpClassType(FullTypeName name) {
        Preconditions.checkNotNull(name);
        if (name.hasPackageName()) {
            return fullyNamedTypes.get(name);
        }

        // search in imported packages
        String simpleName = name.getTypeName();
        FullTypeName imported = resolveImportAlias(simpleName);
        ClassType clazz;
        if (imported != null && (clazz = fullyNamedTypes.get(imported)) != null) {
            return clazz;
        }

        // search in same package
        if (!Strings.isNullOrEmpty(packageScope) &&
                (clazz = fullyNamedTypes.get(FullTypeName.of(packageScope, simpleName))) != null) {
            return clazz;
        }

        // search in default package
        return fullyNamedTypes.get(name);
    }

    public void registerClassType(ClassType classType) {
        if (frames.isEmpty()) {
            throw new IllegalStateException("No frames");
        }
        Preconditions.checkNotNull(classType);
        fullyNamedTypes.put(classType.getName(), classType);
        currentFrame().registerClassType(classType);
    }

    public Frame currentFrame() {
        return frames.getFirst();
    }

    public void enterCompilationUnit(String pkg) {
        Preconditions.checkState(packageScope == null, "Cannot enterCompilationUnit twice");
        packageScope = pkg;
        imports = Maps.newHashMap();
        enterScope();
    }

    public void leaveCompilationUnit() {
        packageScope = null;
        imports = null;
        leaveScope();
    }

    public void enterScope() {
        frames.addFirst(new Frame());
    }

    public void leaveScope() {
        frames.removeFirst();
    }

    public void registerImportPackage(String pkg) {
        Preconditions.checkNotNull(pkg);
        for (Frame frame : frames) {
            for (ClassType clazz : frame.getClasses()) {
                if (Objects.equal(clazz.getName().getPackageName(), pkg)) {
                    registerImportClass(clazz.getHandle());
                }
            }
        }
    }

    public void registerImportClass(TypeHandle classHandle) {
        Preconditions.checkNotNull(classHandle);
        FullTypeName name = classHandle.getName();
        imports.put(name.getTypeName(), name);
    }

    private FullTypeName resolveImportAlias(String alias) {
        FullTypeName ret = imports.get(alias);
        return ret;
    }
}
