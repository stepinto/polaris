package com.codingstory.polaris.parser;

import com.codingstory.polaris.JumpTarget;
import com.google.common.base.Preconditions;

import java.util.EnumSet;
import java.util.Set;

public class Field {
    private final FieldHandle handle;
    private final TypeHandle type;
    private final Set<Modifier> modifiers;
    private final JumpTarget jumpTarget;

    public Field(FieldHandle handle, TypeHandle type, Set<Modifier> modifiers, JumpTarget jumpTarget) {
        this.handle = Preconditions.checkNotNull(handle);
        this.type = Preconditions.checkNotNull(type);
        this.modifiers = Preconditions.checkNotNull(modifiers);
        this.jumpTarget = Preconditions.checkNotNull(jumpTarget);
    }

    public static Field createFromThrift(TField t) {
        Preconditions.checkNotNull(t);
        return new Field(
                FieldHandle.createFromThrift(t.getHandle()),
                TypeHandle.createFromThrift(t.getType()),
                EnumSet.noneOf(Modifier.class),
                JumpTarget.createFromThrift(t.getJumpTarget()));
    }

    public FieldHandle getHandle() {
        return handle;
    }

    public FullMemberName getName() {
        return handle.getName();
    }

    public TypeHandle getType() {
        return type;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public JumpTarget getJumpTarget() {
        return jumpTarget;
    }

    public TField toThrift() {
        TField t = new TField();
        t.setHandle(handle.toThrift());
        t.setType(type.toThrift());
        t.setJumpTarget(jumpTarget.toThrift());
        return t;
    }
}
