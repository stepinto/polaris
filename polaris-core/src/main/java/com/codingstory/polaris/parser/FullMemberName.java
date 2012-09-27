package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Represents a fully qualified member name. It is like "package.class.member".
 */
public class FullMemberName {
    private final FullyQualifiedTypeName fullTypeName;
    private final String memberName;

    public FullMemberName(FullyQualifiedTypeName fullTypeName, String memberName) {
        this.fullTypeName = fullTypeName;
        this.memberName = Preconditions.checkNotNull(memberName);
    }

    public static FullMemberName of(String packageName, String typeName, String memberName) {
        Preconditions.checkNotNull(memberName);
        FullyQualifiedTypeName fullTypeName;
        if (typeName == null) {
            fullTypeName = null;
        } else {
            fullTypeName = FullyQualifiedTypeName.of(packageName, typeName);
        }
        return new FullMemberName(fullTypeName, memberName);
    }

    public static FullMemberName of(FullyQualifiedTypeName fullTypeName, String memberName) {
        return new FullMemberName(fullTypeName, memberName);
    }

    public static FullMemberName of(String name) {
        Preconditions.checkNotNull(name);
        int lastDot = name.lastIndexOf('.');
        FullyQualifiedTypeName fullTypeName;
        String memberName;
        if (lastDot == -1) {
            fullTypeName = null;
            memberName = name;
        } else {
            fullTypeName = FullyQualifiedTypeName.of(name.substring(0, lastDot));
            memberName = name.substring(lastDot + 1);
        }
        return new FullMemberName(fullTypeName, memberName);
    }

    public FullyQualifiedTypeName getFullTypeName() {
        return fullTypeName;
    }

    public String getMemberName() {
        return memberName;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fullTypeName, memberName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FullMemberName)) {
            return false;
        }

        FullMemberName that = (FullMemberName) o;
        return Objects.equal(this.fullTypeName, that.fullTypeName)
                && Objects.equal(this.memberName, that.memberName);
    }

    @Override
    public String toString() {
        return fullTypeName.toString() + "." + memberName;
    }
}
