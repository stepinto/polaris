package com.codingstory.polaris.parser;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import java.util.Map;

public class PrimitiveTypeResolver implements TypeResolver {

    private static final Map<String, ResolvedTypeReference> TABLE = Maps.uniqueIndex(ResolvedTypeReference.PRIMITIVES,
            new Function<ResolvedTypeReference, String>() {
                @Override
                public String apply(ResolvedTypeReference typeReference) {
                    return typeReference.getName().toString();
                }
            });

    public static ResolvedTypeReference resolve(String symbol) {
        return TABLE.get(symbol);
    }

    @Override
    public ResolvedTypeReference resolve(UnresolvedTypeReferenece typeReferenece) {
        return resolve(typeReferenece.getUnqualifiedName());
    }
}
