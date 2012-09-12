package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import java.util.List;

public class MethodDeclaration extends TokenBase {
    private final String packageName;
    private final String className;
    private final String methodName;
    private final TypeReference returnType;
    private final List<Parameter> parameters;
    private final List<TypeReference> exceptions;

    public static class Parameter {
        private final TypeReference type;
        private final String name;

        public Parameter(TypeReference type, String name) {
            this.type = type;
            this.name = name;
        }

        public TypeReference getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Parameter)) {
                return false;
            }
            Parameter that = (Parameter) o;
            return Objects.equal(this.type, that.type)
                    && Objects.equal(this.name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(type, name);
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(Parameter.class)
                    .add("type", type)
                    .add("name", name)
                    .toString();
        }
    }

    public static class Builder {
        private Span span;
        private String packageName;
        private String className;
        private String methodName;
        private TypeReference returnType;
        private List<Parameter> parameters;
        private List<TypeReference> exceptions;

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

        public Builder setReturnType(TypeReference returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder setParameters(List<Parameter> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder setExceptions(List<TypeReference> exceptions) {
            this.exceptions = exceptions;
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
        this.returnType = builder.returnType;
        this.parameters = builder.parameters;
        this.exceptions = builder.exceptions;
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

    public TypeReference getReturnType() {
        return returnType;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public List<TypeReference> getExceptions() {
        return exceptions;
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
