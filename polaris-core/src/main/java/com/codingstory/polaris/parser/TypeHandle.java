package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public final class TypeHandle {
    private final long id;
    private final FullTypeName name;

    public TypeHandle(long id, FullTypeName name) {
        this.id = id; // maybe invalid if the handle is unresolved
        this.name = Preconditions.checkNotNull(name);
    }

    public static TypeHandle createFromThrift(TTypeHandle t) {
        Preconditions.checkNotNull(t);
        return new TypeHandle(t.getId(), FullTypeName.of(t.getName()));
    }

    public static TypeHandle createUnresolved(FullTypeName name) {
        return new TypeHandle(Type.UNRESOLVED_TYPE_ID, Preconditions.checkNotNull(name));
    }

    public long getId() {
        return id;
    }

    public FullTypeName getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != TypeHandle.class) {
            return false;
        }
        TypeHandle that = (TypeHandle) o;
        return this.id == that.id && Objects.equal(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name);
    }

    public TTypeHandle toThrift() {
        TTypeHandle t = new TTypeHandle();
        t.setId(id);
        t.setName(name.toString());
        return t;
    }

    public boolean isResolved() {
        return id != Type.UNRESOLVED_TYPE_ID;
    }

    @Override
    public String toString() {
        if (isResolved()) {
            return String.format("%s@%d", name, id);
        } else {
            return String.format("%s@unresolved", name);
        }
    }
}
