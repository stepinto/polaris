package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Represents a fully qualified member name. It is like "package.class.member".
 */
public final class FullMemberName {
    private final FullTypeName fullTypeName;
    private final String memberName;

    public FullMemberName(FullTypeName fullTypeName, String memberName) {
        this.fullTypeName = fullTypeName;
        this.memberName = Preconditions.checkNotNull(memberName);
    }

    public static FullMemberName of(String packageName, String typeName, String memberName) {
        Preconditions.checkNotNull(memberName);
        FullTypeName fullTypeName;
        if (typeName == null) {
            fullTypeName = null;
        } else {
            fullTypeName = FullTypeName.of(packageName, typeName);
        }
        return new FullMemberName(fullTypeName, memberName);
    }

    public static FullMemberName of(FullTypeName fullTypeName, String memberName) {
        return new FullMemberName(fullTypeName, memberName);
    }

    public static FullMemberName of(String name) {
        Preconditions.checkNotNull(name);
        int lastPound = name.lastIndexOf('#');
        FullTypeName fullTypeName;
        String memberName;
        if (lastPound == -1) {
            fullTypeName = null;
            memberName = name;
        } else {
            fullTypeName = FullTypeName.of(name.substring(0, lastPound));
            memberName = name.substring(lastPound + 1);
        }
        return new FullMemberName(fullTypeName, memberName);
    }

    public FullTypeName getFullTypeName() {
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
        if (o.getClass() != FullMemberName.class) {
            return false;
        }

        FullMemberName that = (FullMemberName) o;
        return Objects.equal(this.fullTypeName, that.fullTypeName)
                && Objects.equal(this.memberName, that.memberName);
    }

    @Override
    public String toString() {
        if (fullTypeName == null) {
            return memberName;
        } else {
            return fullTypeName.toString() + "#" + memberName;
        }
    }
}
