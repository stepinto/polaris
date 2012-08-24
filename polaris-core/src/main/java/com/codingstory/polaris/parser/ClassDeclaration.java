package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;


public class ClassDeclaration extends TokenBase implements TypeDeclaration, JavaDocable {
    private FullyQualifiedName name;
    private String javaDocComment;

    public static class Builder {
        private Span span;
        private FullyQualifiedName name;
        private String javaDocComment;

        public Builder setSpan(Span span) {
            this.span = span;
            return this;
        }

        public Builder setName(FullyQualifiedName name) {
            this.name = name;
            return this;
        }

        public Builder setJavaDocComment(String javaDocComment) {
            this.javaDocComment = javaDocComment;
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
        this.javaDocComment = builder.javaDocComment;
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
    public boolean hasJavaDocComment() {
        return !Strings.isNullOrEmpty(javaDocComment);
    }

    @Override
    public String getJavaDocComment() {
        return javaDocComment;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(ClassDeclaration.class)
                .add("span", getSpan())
                .add("name", name)
                .toString();
    }
}
