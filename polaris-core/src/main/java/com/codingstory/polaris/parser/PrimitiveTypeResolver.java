package com.codingstory.polaris.parser;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.Map;

public class PrimitiveTypeResolver implements TypeResolver {

    private static final Map<FullTypeName, PrimitiveType> TABLE = Maps.uniqueIndex(
            ImmutableList.copyOf(PrimitiveType.values()),
            new Function<PrimitiveType, FullTypeName>() {
                @Override
                public FullTypeName apply(PrimitiveType type) {
                    return type.getName();
                }
            });
    private static final PrimitiveTypeResolver INSTANCE = new PrimitiveTypeResolver();

    private PrimitiveTypeResolver() {}

    @Override
    public TypeHandle resolve(FullTypeName name) {
        Preconditions.checkNotNull(name);
        PrimitiveType type = TABLE.get(name);
        return type != null ? type.getHandle() : null;
    }

    public static PrimitiveTypeResolver getInstance() {
        return INSTANCE;
    }
}
