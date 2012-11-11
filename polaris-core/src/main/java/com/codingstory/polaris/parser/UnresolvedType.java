package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;

public class UnresolvedType implements Type {
    private final FullTypeName name;

    public UnresolvedType(FullTypeName name) {
        this.name = Preconditions.checkNotNull(name);
    }

    @Override
    public TypeHandle getHandle() {
        return new TypeHandle(Type.UNRESOLVED_TYPE_ID, name);
    }

    @Override
    public FullTypeName getName() {
        return name;
    }

    @Override
    public String toString() {
        return "UnresolvedType(\"" + name.toString() + "\")";
    }
}
