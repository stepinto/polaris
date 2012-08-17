package com.codingstory.polaris.parser;

public class MethodDeclaration implements Term {

    private final String packageName;
    private final String className;
    private final String methodName;

    public static class Builder {

        String packageName;
        String className;
        String methodName;

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
            return new MethodDeclaration(this);
        }
    }

    private MethodDeclaration(Builder builder) {
        this.packageName = builder.packageName;
        this.className = builder.className;
        this.methodName = builder.methodName;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public Kind getKind() {
        return Kind.METHOD_DECLARATION;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }
}
