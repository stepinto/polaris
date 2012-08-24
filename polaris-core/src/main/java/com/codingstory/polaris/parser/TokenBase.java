package com.codingstory.polaris.parser;

abstract class TokenBase implements Token {

    private final Kind kind;
    private final Span span;

    protected TokenBase(Kind kind, Span span) {
        this.kind = kind;
        this.span = span;
    }

    public Kind getKind() {
        return kind;
    }

    public Span getSpan() {
        return span;
    }
}
