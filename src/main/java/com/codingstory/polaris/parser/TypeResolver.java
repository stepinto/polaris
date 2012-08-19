package com.codingstory.polaris.parser;

/**
 * Resolvers an {@link UnresolvedTypeReferenece}.
 */
public interface TypeResolver {
    /**
     * @param typeReferenece type to resolve
     * @return the resolved type or null if it cannot be resolved
     */
    public ResolvedTypeReference resolve(UnresolvedTypeReferenece typeReferenece);
}
