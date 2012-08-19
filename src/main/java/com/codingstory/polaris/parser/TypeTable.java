package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.util.Map;

public class TypeTable<T> {

    private final TypeTable<T> parent;
    private final Map<String, T> unqualifiedNameTable = Maps.newHashMap();
    private final Map<FullyQualifiedName, T> fullyQualifiedNameTable = Maps.newHashMap();

    public TypeTable() {
        this(null);
    }

    public TypeTable(TypeTable parent) {
        this.parent = parent;
    }

    public T resolve(String symbol) {
        Preconditions.checkNotNull(symbol);
        FullyQualifiedName qualified = FullyQualifiedName.of(symbol);
        if (qualified.getPackageName() != null) {
            return resolveFullyQualified(qualified);
        } else {
            T result = resolveUnqualified(symbol);
            if (result != null) {
                return result;
            }
            return resolveFullyQualified(qualified);
        }
    }

    private T resolveUnqualified(String name) {
        T result = unqualifiedNameTable.get(name);
        if (result == null && parent != null) {
            return parent.resolveUnqualified(name);
        } else {
            return result;
        }
    }

    private T resolveFullyQualified(FullyQualifiedName name) {
        T result = fullyQualifiedNameTable.get(name);
        if (result == null && parent != null) {
            return parent.resolveFullyQualified(name);
        } else {
            return result;
        }
    }

    public void put(FullyQualifiedName symbol, T value) {
        unqualifiedNameTable.put(symbol.getTypeName(), value);
        fullyQualifiedNameTable.put(symbol, value);
    }

}
