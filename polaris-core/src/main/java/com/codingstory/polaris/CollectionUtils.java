package com.codingstory.polaris;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;

public final class CollectionUtils {

    private CollectionUtils() {}

    public static <T> List<T> nullToEmptyList(List<T> in) {
        if (in == null) {
            return ImmutableList.of();
        } else {
            return in;
        }
    }

    public static <T> Collection<T> nullToEmptyCollection(Collection<T> in) {
        if (in == null) {
            return ImmutableList.of();
        } else {
            return in;
        }
    }
}
