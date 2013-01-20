package com.codingstory.polaris.parser;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.Map;

public final class TypeResolver {

    private static final Map<FullTypeName, PrimitiveType> TABLE = Maps.uniqueIndex(
            ImmutableList.copyOf(PrimitiveType.values()),
            new Function<PrimitiveType, FullTypeName>() {
                @Override
                public FullTypeName apply(PrimitiveType type) {
                    return type.getName();
                }
            });
    private static final TypeResolver INSTANCE = new TypeResolver();

    private TypeResolver() {}

    public static PrimitiveType resolvePrimitiveType(FullTypeName name) {
        Preconditions.checkNotNull(name);
        return TABLE.get(name);
    }
}
