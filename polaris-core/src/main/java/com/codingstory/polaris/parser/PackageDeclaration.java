package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class PackageDeclaration extends TokenBase {

    private final String packageName;

    public static class Builder {
        private Span span;
        private String packageName;

        public Builder setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder setSpan(Span span) {
            this.span = span;
            return this;
        }

        public PackageDeclaration build() {
            Preconditions.checkNotNull(span);
            return new PackageDeclaration(this);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private PackageDeclaration(Builder builder) {
        super(Kind.PACKAGE_DECLARATION, builder.span);
        this.packageName = builder.packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(PackageDeclaration.class)
                .add("span", getSpan())
                .add("packageName", packageName)
                .toString();
    }
}
