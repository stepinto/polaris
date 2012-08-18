package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;


public class ClassDeclaration extends TokenBase implements TypeDeclaration {

    private final String packageName;
    private final String className;

    public static class Builder {
        private Span span;
        private String packageName;
        private String className;

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
        this.packageName = builder.packageName;
        this.className = builder.className;
    }

    @Override
    public Kind getKind() {
        return Kind.CLASS_DECLARATION;
    }

    /**
     * @return the package name or null if it does not have one
     */
    @Override
    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String getTypeName() {
        return className;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(ClassDeclaration.class)
                .add("span", getSpan())
                .add("packageName", packageName)
                .add("className", className)
                .toString();
    }
}
