package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;

public final class Span implements Comparable<Span> {
    private final long from;
    private final long to;

    public Span(long from, long to) {
        this.from = from;
        this.to = to;
    }

    public static Span createFromThrift(TSpan t) {
        Preconditions.checkNotNull(t);
        return new Span(t.getFrom(), t.getTo());
    }

    public static Span of(long from, long to) {
        Preconditions.checkArgument(from >= 0);
        Preconditions.checkArgument(to >= 0);
        Preconditions.checkArgument(from <= to);
        return new Span(from, to);
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
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
        if (o.getClass() != Span.class) {
            return false;
        }
        Span that = (Span) o;
        return this.from == that.from && this.to == that.to;
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
        t.setFrom(from);
        t.setTo(to);
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
