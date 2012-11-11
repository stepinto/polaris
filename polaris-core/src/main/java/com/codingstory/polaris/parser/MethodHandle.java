package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.List;

public final class MethodHandle {
    private final long id;
    private final FullMemberName name;
    private final List<TypeHandle> parameters;

    public MethodHandle(long id, FullMemberName name, List<TypeHandle> parameters) {
        this.id = id;
        this.name = Preconditions.checkNotNull(name);
        this.parameters = Preconditions.checkNotNull(parameters);
    }

    public static MethodHandle createFromThrift(TMethodHandle t) {
        Preconditions.checkNotNull(t);
        List<TypeHandle> parameters = Lists.newArrayList();
        if (t.isSetParameters()) {
            for (TTypeHandle tp : t.getParameters()) {
                parameters.add(TypeHandle.createFromThrift(tp));
            }
        }
        return new MethodHandle(t.getId(), FullMemberName.of(t.getName()), parameters);
    }

    public long getId() {
        return id;
    }

    public FullMemberName getName() {
        return name;
    }

    public List<TypeHandle> getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != MethodHandle.class) {
            return false;
        }
        MethodHandle that = (MethodHandle) o;
        return this.id == that.id
                && Objects.equal(this.name, that.name)
                && Objects.equal(this.parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name, parameters);
    }

    public TMethodHandle toThrift() {
        TMethodHandle t = new TMethodHandle();
        t.setId(id);
        t.setName(name.toString());
        for (TypeHandle parameter : parameters) {
            t.addToParameters(parameter.toThrift());
        }
        return t;
    }
}
