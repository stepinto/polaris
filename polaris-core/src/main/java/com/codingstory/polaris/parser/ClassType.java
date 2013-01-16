package com.codingstory.polaris.parser;

import com.codingstory.polaris.JumpTarget;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Class, interface, enum or annotation. */
public class ClassType implements Type {
    public static enum Kind {
        CLASS,
        INTERFACE,
        ENUM,
        ANNOTATION;

        private static final BiMap<Kind, TClassTypeKind> THRIFT_ENUM_MAP = ImmutableBiMap.of(
                CLASS, TClassTypeKind.CLASS,
                INTERFACE, TClassTypeKind.INTERFACE,
                ENUM, TClassTypeKind.ENUM,
                ANNOTATION, TClassTypeKind.ANNOTATION);

        public static Kind createFromThrift(TClassTypeKind kind) {
            return THRIFT_ENUM_MAP.inverse().get(kind);
        }

        public TClassTypeKind toThrift() {
            return THRIFT_ENUM_MAP.get(this);
        }
    }

    private final TypeHandle handle;
    private final Kind kind;
    private final List<TypeHandle> superTypes;
    private final Set<Modifier> modifiers;
    private final List<Field> fields;
    private final List<Method> methods;
    private final String javaDoc; // nullable
    private final JumpTarget jumpTarget;

    public ClassType(
            TypeHandle handle,
            Kind kind,
            Set<Modifier> modifiers,
            String javaDoc,
            JumpTarget jumpTarget) {
        this.handle = Preconditions.checkNotNull(handle);
        this.kind = Preconditions.checkNotNull(kind);
        this.superTypes = Lists.newArrayList();
        this.modifiers = Preconditions.checkNotNull(modifiers);
        this.fields = Lists.newArrayList();
        this.methods = Lists.newArrayList();
        this.javaDoc = javaDoc;
        this.jumpTarget = Preconditions.checkNotNull(jumpTarget);
    }

    public static ClassType createFromThrift(TClassType t) {
        Preconditions.checkNotNull(t);
        ClassType clazz = new ClassType(
                TypeHandle.createFromThrift(t.getHandle()),
                Kind.createFromThrift(t.getKind()),
                EnumSet.noneOf(Modifier.class),
                t.getJavaDoc(),
                JumpTarget.createFromThrift(t.jumpTarget));
        if (t.isSetSuperTypes()) {
            for (TTypeHandle ts : t.getSuperTypes()) {
                clazz.addSuperType(TypeHandle.createFromThrift(ts));
            }
        }
        if (t.isSetMethods()) {
            for (TMethod tmethod : t.getMethods()) {
                clazz.addMethod(Method.createFromThrift(tmethod));
            }
        }
        if (t.isSetFields()) {
            for (TField tfield : t.getFields()) {
                clazz.addField(Field.createFromThrift(tfield));
            }
        }
        return clazz;
    }

    @Override
    public TypeHandle getHandle() {
        return handle;
    }

    @Override
    public FullTypeName getName() {
        return handle.getName();
    }

    public Kind getKind() {
        return kind;
    }

    public List<TypeHandle> getSuperTypes() {
        return superTypes;
    }

    public void addSuperType(TypeHandle superType) {
        superTypes.add(Preconditions.checkNotNull(superType));
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void addField(Field field) {
        fields.add(Preconditions.checkNotNull(field));
    }

    public List<Method> getMethods() {
        return methods;
    }

    public void addMethod(Method method) {
        methods.add(Preconditions.checkNotNull(method));
    }

    public String getJavaDoc() {
        return javaDoc;
    }

    public JumpTarget getJumpTarget() {
        return jumpTarget;
    }

    @Override
    public String toString() {
        return "ClassType(\"" + handle.getName() + "\")";
    }

    public TClassType toThrift() {
        TClassType t = new TClassType();
        t.setHandle(handle.toThrift());
        t.setKind(kind.toThrift());
        for (TypeHandle superType : superTypes) {
            t.addToSuperTypes(superType.toThrift());
        }
        for (Field field : fields) {
            t.addToFields(field.toThrift());
        }
        for (Method method : methods) {
            t.addToMethods(method.toThrift());
        }
        t.setJavaDoc(javaDoc);
        t.setJumpTarget(jumpTarget.toThrift());
        return t;
    }
}
