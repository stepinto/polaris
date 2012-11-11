package com.codingstory.polaris.parser;

public interface Type {
    static long UNRESOLVED_TYPE_ID = -1;
    TypeHandle getHandle();
    FullTypeName getName();
}
