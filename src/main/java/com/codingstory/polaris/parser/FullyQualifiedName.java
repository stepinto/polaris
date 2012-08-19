package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class FullyQualifiedName {

    private final String packageName;
    private final String typeName;

    private FullyQualifiedName(String packageName, String typeName) {
        this.packageName = packageName;
        this.typeName = typeName;
    }

    public static FullyQualifiedName of(String packageName, String typeName) {
        Preconditions.checkNotNull(typeName);
        return new FullyQualifiedName(packageName, typeName);
    }

    /**
     * @return the fully qualified name or null if it is unqualified
     */
    public static FullyQualifiedName of(String name) {
        Preconditions.checkNotNull(name);
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            return new FullyQualifiedName(null, name);
        }
        String packageName = name.substring(0, lastDot);
        String typeName = name.substring(lastDot + 1);
        // TODO: Check for empty packageName/typeName.
        return new FullyQualifiedName(packageName, typeName);
    }

    public boolean hasPackageName() {
        return packageName != null;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FullyQualifiedName)) {
            return false;
        }

        FullyQualifiedName that = (FullyQualifiedName) o;
        return Objects.equal(this.packageName, that.packageName)
                && Objects.equal(this.typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(packageName, typeName);
    }

    @Override
    public String toString() {
        if (packageName == null) {
            return typeName;
        } else {
            return packageName + "." + typeName;
        }
    }
}
