package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import javax.validation.constraints.NotNull;
import java.util.EnumSet;
import java.util.Set;


public class TypeDeclaration extends TokenBase implements JavaDocable {
    private static final Set<Kind> VALID_KINDS = EnumSet.of(
            Kind.CLASS_DECLARATION,
            Kind.INTERFACE_DECLARATION,
            Kind.ENUM_DECLARATION,
            Kind.ANNOTATION_DECLARATION);
    private final FullyQualifiedTypeName name;
    private final String javaDocComment;

    public static class Builder {
        private Kind kind;
        private Span span;
        private FullyQualifiedTypeName name;
        private String javaDocComment;

        public Builder setKind(@NotNull Kind kind) {
            Preconditions.checkArgument(VALID_KINDS.contains(kind));
            this.kind = kind;
            return this;
        }

        public Builder setSpan(@NotNull Span span) {
            this.span = span;
            return this;
        }

        public Builder setName(@NotNull FullyQualifiedTypeName name) {
            Preconditions.checkNotNull(name);
            this.name = name;
            return this;
        }

        public Builder setJavaDocComment(String javaDocComment) {
            this.javaDocComment = javaDocComment;
            return this;
        }

        public TypeDeclaration build() {
            Preconditions.checkNotNull(kind);
            Preconditions.checkNotNull(span);
            return new TypeDeclaration(this);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private TypeDeclaration(Builder builder) {
        super(builder.kind, builder.span);
        this.name = builder.name;
        this.javaDocComment = builder.javaDocComment;
    }

    public FullyQualifiedTypeName getName() {
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
        return Objects.toStringHelper(TypeDeclaration.class)
                .add("kind", getKind())
                .add("span", getSpan())
                .add("name", name)
                .toString();
    }
}
