package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.LinkedList;
import java.util.Map;

/**
 * Symbol table for types.
 */
public class TypeTable {
    public static class Frame {
        private final Map<FullTypeName, TypeHandle> fullyNamedTypes = Maps.newHashMap();
        private final Map<String, TypeHandle> shortlyNamedTypes = Maps.newHashMap();

        public void put(TypeHandle type) {
            Preconditions.checkNotNull(type);
            FullTypeName typeName = type.getName();
            fullyNamedTypes.put(typeName, type);
            shortlyNamedTypes.put(typeName.getTypeName(), type);
        }

        public TypeHandle lookUp(FullTypeName name) {
            Preconditions.checkNotNull(name);
            if (name.hasPackageName()) {
                return fullyNamedTypes.get(name);
            } else {
                return shortlyNamedTypes.get(name.getTypeName());
            }
        }
    }

    private final LinkedList<Frame> frames = Lists.newLinkedList();

    public TypeHandle lookUp(FullTypeName name) {
        Preconditions.checkNotNull(name);
        for (Frame frame : frames) {
            TypeHandle result = frame.lookUp(name);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public void put(TypeHandle type) {
        if (frames.isEmpty()) {
            throw new IllegalStateException("No frames");
        }
        currentFrame().put(type);
    }

    public Frame currentFrame() {
        return frames.getFirst();
    }

    public void enterFrame() {
        frames.addFirst(new Frame());
    }

    public void leaveFrame() {
        frames.removeFirst();
    }

    public TypeResolver getTypeResolver() {
        return new TypeResolver() {
            @Override
            public TypeHandle resolve(FullTypeName name) {
                Preconditions.checkNotNull(name);
                return lookUp(name);
            }
        };
    }
}
