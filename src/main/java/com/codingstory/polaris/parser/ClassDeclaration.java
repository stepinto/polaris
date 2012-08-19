package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;


public class ClassDeclaration extends TokenBase implements TypeDeclaration {

    private FullyQualifiedName name;

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

        public ClassDeclaration build() {
            Preconditions.checkNotNull(span);
            return new ClassDeclaration(this);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private ClassDeclaration(Builder builder) {
        super(Kind.CLASS_DECLARATION, builder.span);
        this.name = builder.name;
    }

    @Override
    public Kind getKind() {
        return Kind.CLASS_DECLARATION;
    }

    @Override
    public FullyQualifiedName getName() {
        return name;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(ClassDeclaration.class)
                .add("span", getSpan())
                .add("name", name)
                .toString();
    }
}
