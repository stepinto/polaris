package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class MethodDeclaration extends TokenBase {

    private final String packageName;
    private final String className;
    private final String methodName;

    public static class Builder {

        Span span;
        String packageName;
        String className;
        String methodName;

        public Builder setSpan(Span span) {
            this.span = span;
            return this;
        }

        public Builder setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder setMethodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder setClassName(String className) {
            this.className = className;
            return this;
        }

        public MethodDeclaration build() {
            Preconditions.checkNotNull(span);
            return new MethodDeclaration(this);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private MethodDeclaration(Builder builder) {
        super(Kind.METHOD_DECLARATION, builder.span);
        this.packageName = builder.packageName;
        this.className = builder.className;
        this.methodName = builder.methodName;
    }

    @Override
    public Kind getKind() {
        return Kind.METHOD_DECLARATION;
    }

    /**
     * @return the package name or null if it does not have one
     */
    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(MethodDeclaration.class)
                .add("span", getSpan())
                .add("packageName", packageName)
                .add("className", className)
                .add("methodName", methodName)
                .toString();
    }
}
