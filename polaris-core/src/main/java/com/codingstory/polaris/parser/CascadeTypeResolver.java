package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.List;

public class CascadeTypeResolver implements TypeResolver {

    private final List<TypeResolver> typeResolvers;

    public CascadeTypeResolver(List<TypeResolver> typeResolvers) {
        this.typeResolvers = Preconditions.checkNotNull(typeResolvers);
    }

    @Override
    public ResolvedTypeReference resolve(UnresolvedTypeReferenece unresolved) throws IOException {
        for (TypeResolver typeResolver : typeResolvers) {
            ResolvedTypeReference resolved = typeResolver.resolve(unresolved);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }
}
