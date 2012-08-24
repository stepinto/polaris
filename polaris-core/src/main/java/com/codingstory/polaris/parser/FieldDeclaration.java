package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class FieldDeclaration extends TokenBase implements VariableDeclaration {

    private final String packageName;
    private final String className;
    private final String variableName;
    private final TypeReference typeReference;

    public static class Builder {
        private Span span;
        private String packageName;
        private String className;
        private String variableName;
        private TypeReference typeReference;

        public Builder setSpan(Span span) {
            this.span = span;
            return this;
        }

        public Builder setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder setClassName(String className) {
            this.className = className;
            return this;
        }

        public Builder setVariableName(String variableName) {
            this.variableName = variableName;
            return this;
        }

        public Builder setTypeReference(TypeReference typeReference) {
            this.typeReference = typeReference;
            return this;
        }

        public FieldDeclaration build() {
            Preconditions.checkNotNull(span);
            return new FieldDeclaration(this);
        }
    }

    public FieldDeclaration(Builder builder) {
        super(Kind.FIELD_DECLARATION, builder.span);
        this.packageName = builder.packageName;
        this.className = builder.className;
        this.variableName = builder.variableName;
        this.typeReference = builder.typeReference;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String getVariableName() {
        return variableName;
    }

    @Override
    public TypeReference getTypeReferenece() {
        return typeReference;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(FieldDeclaration.class)
                .add("packageName", packageName)
                .add("className", className)
                .add("variableName", variableName)
                .add("typeReference", typeReference)
                .toString();
    }
}
