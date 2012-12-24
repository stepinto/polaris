package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;

public final class Span implements Comparable<Span> {
    private final Position from;
    private final Position to;

    public Span(Position from, Position to) {
        this.from = from;
        this.to = to;
    }

    public static Span createFromThrift(TSpan t) {
        Preconditions.checkNotNull(t);
        return new Span(
                Position.createFromThrift(t.getFrom()),
                Position.createFromThrift(t.getTo()));
    }

    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(from, to);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o.getClass() != Span.class) {
            return false;
        }
        Span that = (Span) o;
        return Objects.equal(this.from, that.from) && Objects.equal(this.to, that.to);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(Span.class)
                .add("from", from)
                .add("to", to)
                .toString();
    }

    public TSpan toThrift() {
        TSpan t = new TSpan();
        t.setFrom(from.toThrift());
        t.setTo(to.toThrift());
        return t;
    }

    @Override
    public int compareTo(Span that) {
        return ComparisonChain.start()
                .compare(this.from, that.from)
                .compare(this.to, that.to)
                .result();
    }
}
