package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public final class FullLocalName {

    private final FullMemberName fullMemberName;
    private final String localName;

    public FullLocalName(FullMemberName fullMemberName, String localName) {
        this.fullMemberName = fullMemberName;
        this.localName = Preconditions.checkNotNull(localName);
    }

    public static FullLocalName of(String packageName, String typeName, String methodName, String localName) {
        return new FullLocalName(FullMemberName.of(packageName, typeName, methodName), localName);
    }

    public static FullLocalName of(FullMemberName memberName, String localName) {
        return new FullLocalName(memberName, localName);
    }

    public static FullLocalName of(String name) {
        int lastDot = name.lastIndexOf('.');
        FullMemberName memberName;
        if (lastDot == -1) {
            memberName = null;
        } else {
            memberName = FullMemberName.of(name.substring(0, lastDot));
        }
        String localName = name.substring(lastDot + 1);
        return new FullLocalName(memberName, localName);
    }

    public FullMemberName getFullMemberName() {
        return fullMemberName;
    }

    public String getLocalName() {
        return localName;
    }

    @Override
    public String toString() {
        return fullMemberName.toString() + "." + localName;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fullMemberName, localName);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o.getClass() != FullLocalName.class) {
            return false;
        }
        FullLocalName that = (FullLocalName) o;
        return Objects.equal(this.fullMemberName, that.fullMemberName)
                && Objects.equal(this.localName, that.localName);
    }
}
