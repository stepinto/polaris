package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public class FieldUsage implements Usage {
    public enum Kind {
        FIELD_DECLARATION,
        FIELD_ACCESS;

        private static final BiMap<Kind, TFieldUsageKind> THRIFT_ENUM_MAP = ImmutableBiMap.of(
                FIELD_DECLARATION, TFieldUsageKind.FIELD_DECLARATION,
                FIELD_ACCESS, TFieldUsageKind.FIELD_ACCESS);

        public static Kind createFromThrift(TFieldUsageKind t) {
            return THRIFT_ENUM_MAP.inverse().get(Preconditions.checkNotNull(t));
        }

        public TFieldUsageKind toThrift() {
            return THRIFT_ENUM_MAP.get(this);
        }
    }

    private FieldHandle field;
    private final Span span;
    private final Kind kind;

    public FieldUsage(FieldHandle field, Span span, Kind kind) {
        this.field = Preconditions.checkNotNull(field);
        this.span = Preconditions.checkNotNull(span);
        this.kind = Preconditions.checkNotNull(kind);
    }

    public FieldUsage createFromThrift(TFieldUsage t) {
        Preconditions.checkNotNull(t);
        return new FieldUsage(
                FieldHandle.createFromThrift(t.getField()),
                Span.createFromThrift(t.getSpan()),
                Kind.createFromThrift(t.getKind()));
    }

    public FieldHandle getField() {
        return field;
    }

    @Override
    public Span getSpan() {
        return span;
    }

    public Kind getKind() {
        return kind;
    }

    public TFieldUsage toThrift() {
        TFieldUsage t = new TFieldUsage();
        t.setField(field.toThrift());
        t.setSpan(span.toThrift());
        t.setKind(kind.toThrift());
        return t;
    }
}
