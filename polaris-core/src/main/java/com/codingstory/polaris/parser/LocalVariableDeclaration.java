package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class LocalVariableDeclaration extends TokenBase implements VariableDeclaration {

    private final FullLocalName name;
    private final TypeReference typeReference;

    public static class Builder {
        private Span span;
        private FullLocalName name;
        private TypeReference typeReference;

        public Builder setSpan(Span span) {
            this.span = span;
            return this;
        }

        public Builder setName(FullLocalName name) {
            this.name = name;
            return this;
        }

        public Builder setTypeReference(TypeReference typeReference) {
            this.typeReference = typeReference;
            return this;
        }

        public LocalVariableDeclaration build() {
            return new LocalVariableDeclaration(this);
        }
    }

    public LocalVariableDeclaration(Builder builder) {
        super(Kind.LOCAL_VARIABLE_DECLARATION, Preconditions.checkNotNull(builder.span));
        this.name = Preconditions.checkNotNull(builder.name);
        this.typeReference = Preconditions.checkNotNull(builder.typeReference);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public FullLocalName getName() {
        return name;
    }

    @Override
    public String getVariableName() {
        return name.getLocalName();
    }

    @Override
    public TypeReference getTypeReference() {
        return typeReference;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("typeReference", typeReference)
                .toString();
    }
}
