package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class TypeUtils {
    private static final Set<TypeHandle> PRIMITIVE_TYPE_HANDLES;

    static {
        ImmutableSet.Builder<TypeHandle> builder = ImmutableSet.builder();
        for (PrimitiveType primitiveType : PrimitiveType.values()) {
            builder.add(primitiveType.getHandle());
        }
        PRIMITIVE_TYPE_HANDLES = builder.build();
    }

    private TypeUtils() {}

    public static boolean isPrimitiveTypeHandle(TypeHandle handle) {
        Preconditions.checkNotNull(handle);
        return PRIMITIVE_TYPE_HANDLES.contains(handle);
    }
}
