package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Represents a fully qualified type name, which is consists of an optional package
 * name and a class/interface/enum name.
 */
public class FullyQualifiedTypeName {

    private final String packageName;
    private final String typeName;

    private FullyQualifiedTypeName(String packageName, String typeName) {
        this.packageName = packageName;
        this.typeName = typeName;
    }

    public static FullyQualifiedTypeName of(String packageName, String typeName) {
        Preconditions.checkNotNull(typeName);
        return new FullyQualifiedTypeName(packageName, typeName);
    }

    public static FullyQualifiedTypeName of(String name) {
        Preconditions.checkNotNull(name);
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            return new FullyQualifiedTypeName(null, name);
        }
        String packageName = name.substring(0, lastDot);
        String typeName = name.substring(lastDot + 1);
        // TODO: Check for empty packageName/typeName.
        return new FullyQualifiedTypeName(packageName, typeName);
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
        if (!(o instanceof FullyQualifiedTypeName)) {
            return false;
        }

        FullyQualifiedTypeName that = (FullyQualifiedTypeName) o;
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
