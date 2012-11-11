package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;

import java.util.List;

public class CascadeTypeResolver implements TypeResolver {

    private final List<TypeResolver> typeResolvers;

    public CascadeTypeResolver(List<TypeResolver> typeResolvers) {
        this.typeResolvers = Preconditions.checkNotNull(typeResolvers);
    }

    @Override
    public TypeHandle resolve(FullTypeName name) {
        Preconditions.checkNotNull(name);
        for (TypeResolver typeResolver : typeResolvers) {
            TypeHandle type = typeResolver.resolve(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }
}
