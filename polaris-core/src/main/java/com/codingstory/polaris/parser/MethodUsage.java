package com.codingstory.polaris.parser;

import com.codingstory.polaris.JumpTarget;
import com.google.common.base.Preconditions;

public class MethodUsage implements Usage {
    public enum Kind {
        METHOD_DECLARATION,
        METHOD_CALL
    }

    private final MethodHandle method;
    private final JumpTarget jumpTarget;
    private final Kind kind;

    public MethodUsage(MethodHandle method, JumpTarget jumpTarget, Kind kind) {
        this.method = Preconditions.checkNotNull(method);
        this.jumpTarget = Preconditions.checkNotNull(jumpTarget);
        this.kind = Preconditions.checkNotNull(kind);
    }

    public MethodHandle getMethod() {
        return method;
    }

    @Override
    public JumpTarget getJumpTarget() {
        return jumpTarget;
    }

    public Kind getKind() {
        return kind;
    }
}
