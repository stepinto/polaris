package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;

public class MethodUsage implements Usage {
    public enum Kind {
        METHOD_DECLARATION,
        METHOD_CALL
    }

    private final MethodHandle method;
    private final Span span;
    private final Kind kind;

    public MethodUsage(MethodHandle method, Span span, Kind kind) {
        this.method = Preconditions.checkNotNull(method);
        this.span = Preconditions.checkNotNull(span);
        this.kind = Preconditions.checkNotNull(kind);
    }

    public MethodHandle getMethod() {
        return method;
    }

    @Override
    public Span getSpan() {
        return span;
    }

    public Kind getKind() {
        return kind;
    }
}
