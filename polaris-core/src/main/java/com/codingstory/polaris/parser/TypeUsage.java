package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;

public class TypeUsage extends TokenBase {
    private final TypeReference typeReference;

    public static class Builder {
        Span span;
        TypeReference typeReference;

        public Builder setSpan(Span span) {
            this.span = Preconditions.checkNotNull(span);
            return this;
        }

        public Builder setTypeReference(TypeReference typeReference) {
            this.typeReference = Preconditions.checkNotNull(typeReference);
            return this;
        }

        public TypeUsage build() {
            Preconditions.checkNotNull(span);
            Preconditions.checkNotNull(typeReference);
            return new TypeUsage(this);
        }
    }

    private TypeUsage(Builder builder) {
        super(Kind.TYPE_USAGE, builder.span);
        this.typeReference = builder.typeReference;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public TypeReference getTypeReference() {
        return typeReference;
    }
}
