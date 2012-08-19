package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;

public class EnumDeclaration extends TokenBase implements TypeDeclaration {

    private final FullyQualifiedName name;

    public static class Builder {
        private Span span;
        private FullyQualifiedName name;

        public Builder setSpan(Span span) {
            this.span = span;
            return this;
        }

        public Builder setName(FullyQualifiedName name) {
            this.name = name;
            return this;
        }

        public EnumDeclaration build() {
            Preconditions.checkNotNull(span);
            return new EnumDeclaration(this);
        }
    }

    private EnumDeclaration(Builder builder) {
        super(Token.Kind.ENUM_DECLARATION, builder.span);
        this.name = builder.name;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public FullyQualifiedName getName() {
        return name;
    }
}
