package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;

public class EnumDeclaration extends TokenBase implements TypeDeclaration {

    private final String packageName;
    private final String enumName;

    public static class Builder {
        private Span span;
        private String packageName;
        private String enumName;

        public Builder setSpan(Span span) {
            this.span = span;
            return this;
        }

        public Builder setEnumName(String enumName) {
            this.enumName = enumName;
            return this;
        }

        public Builder setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public EnumDeclaration build() {
            Preconditions.checkNotNull(span);
            return new EnumDeclaration(this);
        }
    }

    private EnumDeclaration(Builder builder) {
        super(Token.Kind.ENUM_DECLARATION, builder.span);
        this.packageName = builder.packageName;
        this.enumName = builder.enumName;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public String getTypeName() {
        return enumName;
    }

    public String getEnumName() {
        return enumName;
    }
}
