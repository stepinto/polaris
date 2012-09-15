package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public interface Token {

    public static enum Kind {
        // To add a new kind enum, you need to update:
        // 1) this file
        // 2) token.thrift
        // 3) PojoToThriftConverter
        // 4) CodeSearchStub
        PACKAGE_DECLARATION,
        CLASS_DECLARATION,
        ENUM_DECLARATION,
        ANNOTATION_DECLARATION,
        INTERFACE_DECLARATION,
        METHOD_DECLARATION,
        FIELD_DECLARATION,
        TYPE_USAGE,
    }

    public static class Span {
        private final long from;
        private final long to;

        private Span(long from, long to) {
            this.from = from;
            this.to = to;
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
            if (!(o instanceof Span)) {
                return false;
            }
            Span that = (Span) o;
            return this.from == that.from
                    && this.to == that.to;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(Span.class)
                    .add("from", from)
                    .add("to", to)
                    .toString();
        }
    }

    public Kind getKind();
    public Span getSpan();

}
