package com.codingstory.polaris.parser;

import com.codingstory.polaris.JumpTarget;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Method {
    public static class Parameter {
        private final TypeHandle type;
        private final String name;

        public Parameter(TypeHandle type, String name) {
            this.type = Preconditions.checkNotNull(type);
            this.name = Preconditions.checkNotNull(name);
        }

        public static Parameter createFromThrift(TMethodParameter t) {
            Preconditions.checkNotNull(t);
            return new Parameter(TypeHandle.createFromThrift(t.getType()), t.getName());
        }

        public TypeHandle getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public TMethodParameter toThrift() {
            TMethodParameter t = new TMethodParameter();
            t.setType(type.toThrift());
            t.setName(name);
            return t;
        }
    }

    private final MethodHandle handle;
    private final TypeHandle returnType;
    private final List<Parameter> parameters;
    private final List<TypeHandle> exceptions;
    private final Set<Modifier> modifiers;
    private final JumpTarget jumpTarget;

    public Method(MethodHandle handle,
            TypeHandle returnType,
            List<Parameter> parameters,
            List<TypeHandle> exceptions,
            Set<Modifier> modifiers,
            JumpTarget jumpTarget) {
        this.handle = Preconditions.checkNotNull(handle);
        this.returnType = Preconditions.checkNotNull(returnType);
        this.parameters = Preconditions.checkNotNull(parameters);
        this.exceptions = Preconditions.checkNotNull(exceptions);
        this.modifiers = Preconditions.checkNotNull(modifiers);
        this.jumpTarget = Preconditions.checkNotNull(jumpTarget);
    }

    public static Method createFromThrift(TMethod t) {
        Preconditions.checkNotNull(t);
        List<Parameter> parameters = Lists.newArrayList();
        if (t.isSetParameters()) {
            for (TMethodParameter tparam : t.getParameters()) {
                parameters.add(Parameter.createFromThrift(tparam));
            }
        }
        List<TypeHandle> exceptions = Lists.newArrayList();
        if (t.isSetExceptions()) {
            for (TTypeHandle tex : t.getExceptions()) {
                exceptions.add(TypeHandle.createFromThrift(tex));
            }
        }
        return new Method(
                MethodHandle.createFromThrift(t.getHandle()),
                TypeHandle.createFromThrift(t.getReturnType()),
                parameters,
                exceptions,
                EnumSet.noneOf(Modifier.class),
                JumpTarget.createFromThrift(t.getJumpTarget()));
    }

    public long getId() {
        return handle.getId();
    }

    public FullMemberName getName() {
        return handle.getName();
    }

    public TypeHandle getReturnType() {
        return returnType;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public List<TypeHandle> getExceptions() {
        return exceptions;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public MethodHandle getHandle() {
        return handle;
    }

    public JumpTarget getJumpTarget() {
        return jumpTarget;
    }

    public TMethod toThrift() {
        TMethod t = new TMethod();
        t.setHandle(handle.toThrift());
        t.setReturnType(returnType.toThrift());
        for (Parameter parameter : parameters) {
            t.addToParameters(parameter.toThrift());
        }
        for (TypeHandle exception : exceptions) {
            t.addToExceptions(exception.toThrift());
        }
        t.setJumpTarget(jumpTarget.toThrift());
        return t;
    }
}
