package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;

/** A pair of line and column. The line and column are numberd from zero. */
public final class Position implements Comparable<Position> {
    public static final Position ZERO = new Position(0, 0);
    private final int line;
    private final int column;

    public Position(int line, int column) {
        Preconditions.checkArgument(line >= 0);
        Preconditions.checkArgument(column >= 0);
        this.line = line;
        this.column = column;
    }

    public static Position createFromThrift(TPosition t) {
        Preconditions.checkNotNull(t);
        return new Position(t.getLine(), t.getColumn());
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o.getClass() != Position.class) {
            return false;
        }

        Position that = (Position) o;
        return this.line == that.line && this.column == that.column;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(line, column);
    }

    @Override
    public int compareTo(Position that) {
        return ComparisonChain.start()
                .compare(this.line, that.line)
                .compare(this.column, that.column)
                .result();
    }

    public TPosition toThrift() {
        TPosition t = new TPosition();
        t.setLine(line);
        t.setColumn(column);
        return t;
    }

    @Override
    public String toString() {
        return "Position(" + line + ", " + column + ")";
    }
}
