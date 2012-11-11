package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public final class TypeUsage implements Usage {
    // TODO: Kind.SUBCLASS, Kind.SUPER_CLASS, Kind.METHOD_SIGNATURE

    public static enum Kind {
        IMPORT,
        SUPER_CLASS,
        METHOD_SIGNATURE, // method parameter, return value, or throws
        FIELD,
        LOCAL_VARIABLE,
        GENERIC_TYPE_PARAMETER,
        TYPE_DECLARATION;

        private static final BiMap<Kind, TTypeUsageKind> THRIFT_ENUM_MAP =
                ImmutableBiMap.<Kind, TTypeUsageKind>builder()
                        .put(IMPORT, TTypeUsageKind.IMPORT)
                        .put(SUPER_CLASS, TTypeUsageKind.SUPER_CLASS)
                        .put(METHOD_SIGNATURE, TTypeUsageKind.METHOD_SIGNATURE)
                        .put(FIELD, TTypeUsageKind.FIELD)
                        .put(LOCAL_VARIABLE, TTypeUsageKind.LOCAL_VARIABLE)
                        .put(GENERIC_TYPE_PARAMETER, TTypeUsageKind.GENERIC_TYPE_PARAMETER)
                        .put(TYPE_DECLARATION, TTypeUsageKind.TYPE_DECLARATION)
                        .build();

        public static Kind createFromThrift(TTypeUsageKind t) {
            Preconditions.checkNotNull(t);
            return THRIFT_ENUM_MAP.inverse().get(t);
        }

        public TTypeUsageKind toThrift() {
            return THRIFT_ENUM_MAP.get(this);
        }
    }

    // TODO: fileinfo
    private final TypeHandle type;
    private final Span span;
    private final Kind kind;

    public TypeUsage(TypeHandle type, Span span, Kind kind) {
        this.type = Preconditions.checkNotNull(type);
        this.span = Preconditions.checkNotNull(span);
        this.kind = Preconditions.checkNotNull(kind);
    }

    public static TypeUsage createFromThrift(TTypeUsage t) {
        Preconditions.checkNotNull(t);
        return new TypeUsage(
                TypeHandle.createFromThrift(t.getType()),
                Span.createFromThrift(t.getSpan()),
                Kind.createFromThrift(t.getKind()));
    }

    public TypeHandle getType() {
        return type;
    }

    @Override
    public Span getSpan() {
        return span;
    }

    public Kind getKind() {
        return kind;
    }

    public TTypeUsage toThrift() {
        TTypeUsage t = new TTypeUsage();
        t.setType(type.toThrift());
        t.setSpan(span.toThrift());
        t.setKind(kind.toThrift());
        return t;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(TypeUsage.class)
                .add("type", type)
                .add("span", span)
                .add("kind", kind)
                .toString();
    }
}
