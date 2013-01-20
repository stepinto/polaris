package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Represents a fully qualified type name, which is consists of an optional package
 * name and a class/interface/enum name.
 */
public final class FullTypeName implements Comparable<FullTypeName> {

    private final String packageName;
    private final String typeName;

    private FullTypeName(String packageName, String typeName) {
        this.packageName = packageName;
        this.typeName = typeName;
    }

    public static FullTypeName of(String packageName, String typeName) {
        Preconditions.checkNotNull(typeName);
        if (packageName != null && packageName.isEmpty()) {
            packageName = null;
        }
        return new FullTypeName(packageName, typeName);
    }

    public static FullTypeName of(String name) {
        Preconditions.checkNotNull(name);
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            return new FullTypeName(null, name);
        }
        String packageName = name.substring(0, lastDot);
        String typeName = name.substring(lastDot + 1);
        // TODO: Check for empty packageName/typeName.
        return new FullTypeName(packageName, typeName);
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
        if (o == null) {
            return false;
        }
        if (o.getClass() != FullTypeName.class) {
            return false;
        }

        FullTypeName that = (FullTypeName) o;
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

    @Override
    public int compareTo(FullTypeName right) {
        return this.toString().compareTo(right.toString());
    }
}
