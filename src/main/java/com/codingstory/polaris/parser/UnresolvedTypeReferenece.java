package com.codingstory.polaris.parser;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.util.List;

public class UnresolvedTypeReferenece implements TypeReference {

    private final List<FullyQualifiedName> candidates;

    public UnresolvedTypeReferenece(List<FullyQualifiedName> candidates) {
        Preconditions.checkNotNull(candidates);
        this.candidates = candidates;
    }

    public List<FullyQualifiedName> getCandidates() {
        return candidates;
    }

    private static boolean checkTypeNamesAreSame(List<FullyQualifiedName> candidates) {
        if (candidates.isEmpty()) {
            return true;
        }
        final String typeName = candidates.get(0).getTypeName();
        return Iterables.any(candidates, new Predicate<FullyQualifiedName>() {
            @Override
            public boolean apply(FullyQualifiedName fullyQualifiedName) {
                return !Objects.equal(typeName, fullyQualifiedName.getTypeName());
            }
        });
    }

    @Override
    public boolean isResoleved() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnresolvedTypeReferenece)) {
            return false;
        }

        UnresolvedTypeReferenece that = (UnresolvedTypeReferenece) o;
        return Objects.equal(this.candidates, that.candidates);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(candidates);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(UnresolvedTypeReferenece.class)
                .add("candidates", candidates)
                .toString();
    }
}
