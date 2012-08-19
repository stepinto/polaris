package com.codingstory.polaris.parser;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import java.util.Map;

public class PrimitiveTypeResolver {

    private static final Map<String, ResolvedTypeReference> TABLE = Maps.uniqueIndex(ResolvedTypeReference.PRIMITIVES,
            new Function<ResolvedTypeReference, String>() {
                @Override
                public String apply(ResolvedTypeReference typeReference) {
                    return typeReference.getName().toString();
                }
            });

    public static TypeReference resolve(String symbol) {
        return TABLE.get(symbol);
    }

}
