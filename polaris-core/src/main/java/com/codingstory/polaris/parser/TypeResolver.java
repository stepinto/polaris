package com.codingstory.polaris.parser;

/**
 * Resolvers an {@link UnresolvedTypeReferenece}.
 */
public interface TypeResolver {
    TypeResolver NO_OP_RESOLVER = new TypeResolver() {
        @Override
        public ResolvedTypeReference resolve(UnresolvedTypeReferenece typeReferenece) {
            return null;
        }
    };

    /**
     * @param typeReferenece type to resolve
     * @return the resolved type or null if it cannot be resolved
     */
    ResolvedTypeReference resolve(UnresolvedTypeReferenece typeReferenece);
}
