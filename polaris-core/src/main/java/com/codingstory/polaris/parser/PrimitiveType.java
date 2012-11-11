package com.codingstory.polaris.parser;

import com.codingstory.polaris.ReservedIdGenerator;

public enum PrimitiveType implements Type {
    VOID("void"),
    BYTE("byte"),
    CHARACTER("char"),
    SHORT("short"),
    INTEGER("int"),
    LONG("long"),
    FLOAT("float"),
    DOUBLE("double");

    private final TypeHandle handle;

    private PrimitiveType(String name) {
        this.handle = new TypeHandle(
                ReservedIdGenerator.generateReservedId(),
                FullTypeName.of(name));
    }

    @Override
    public TypeHandle getHandle() {
        return handle;
    }

    @Override
    public FullTypeName getName() {
        return handle.getName();
    }

    @Override
    public String toString() {
        return handle.getName().toString();
    }
}
