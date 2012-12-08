package com.codingstory.polaris;

import com.codingstory.polaris.parser.TFileHandle;
import com.google.common.collect.ImmutableSet;

import java.util.List;

import static junit.framework.Assert.assertEquals;

public class TestUtils {
    private TestUtils() {}

    public static <T> void assertEqualsIgnoreOrder(Iterable<T> expected, Iterable<T> actual) {
        assertEquals(ImmutableSet.copyOf(expected), ImmutableSet.copyOf(actual));
    }
}
