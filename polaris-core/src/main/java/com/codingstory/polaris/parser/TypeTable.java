package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.LinkedList;
import java.util.Map;

/**
 * Symbol table for types.
 */
public class TypeTable<T> {

    private static class Frame<T> {
        private final Map<String, T> unqualifiedNameTable = Maps.newHashMap();
        private final Map<FullyQualifiedTypeName, T> fullyQualifiedNameTable = Maps.newHashMap();

        public void put(FullyQualifiedTypeName name, T value) {
            Preconditions.checkNotNull(name);
            Preconditions.checkNotNull(value);
            unqualifiedNameTable.put(name.getTypeName(), value);
            // Collisions may happen if the source file does not compile. It is ignored here
            // because Polaris is not going to be a compiler.
            fullyQualifiedNameTable.put(name, value);
        }

        public T lookUp(String symbol) {
            Preconditions.checkNotNull(symbol);
            FullyQualifiedTypeName name = FullyQualifiedTypeName.of(symbol);
            if (name.hasPackageName()) {
                return fullyQualifiedNameTable.get(name);
            } else {
                return unqualifiedNameTable.get(symbol);
            }
        }
    }

    private final LinkedList<Frame<T>> frames = Lists.newLinkedList();

    public T lookUp(String symbol) {
        Preconditions.checkNotNull(symbol);
        for (Frame<T> frame : frames) {
            T result = frame.lookUp(symbol);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public void put(FullyQualifiedTypeName name, T value) {
        if (frames.isEmpty()) {
            throw new IllegalStateException("No frames");
        }
        frames.getFirst().put(name, value);
    }

    public void enterFrame() {
        frames.addFirst(new Frame());
    }

    public void leaveFrame() {
        frames.removeFirst();
    }
}
