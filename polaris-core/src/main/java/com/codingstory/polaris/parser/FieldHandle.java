package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public final class FieldHandle {
    private final long id;
    private final FullMemberName name;

    public FieldHandle(long id, FullMemberName name) {
        this.id = id;
        this.name = Preconditions.checkNotNull(name);
    }

    public static FieldHandle createFromThrift(TFieldHandle t) {
        Preconditions.checkNotNull(t);
        return new FieldHandle(t.getId(), FullMemberName.of(t.getName()));
    }

    public long getId() {
        return id;
    }

    public FullMemberName getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != FieldHandle.class) {
            return false;
        }
        FieldHandle that = (FieldHandle) o;
        return this.id == that.id && Objects.equal(this.name, that.name);
    }

    public TFieldHandle toThrift() {
        TFieldHandle t = new TFieldHandle();
        t.setId(id);
        t.setName(name.toString());
        return t;
    }
}
