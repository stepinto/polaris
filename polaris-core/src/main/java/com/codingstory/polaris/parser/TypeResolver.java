package com.codingstory.polaris.parser;

public interface TypeResolver {
    TypeResolver NO_OP_RESOLVER = new TypeResolver() {
        @Override
        public TypeHandle resolve(FullTypeName name) {
            return null;
        }
    };

    /**
     * @param name type to resolve
     * @return the resolved type or null if it cannot be resolved
     */
    TypeHandle resolve(FullTypeName name);
}
