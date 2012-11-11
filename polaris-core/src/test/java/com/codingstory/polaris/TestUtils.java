package com.codingstory.polaris;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class TestUtils {
    private TestUtils() {}
    public static <T> void assertEqualsIgnoreOrder(Iterable<T> expected, Iterable<T> actual) {
        List<T> left = Lists.newArrayList(expected);
        List<T> right = Lists.newArrayList(actual);
        String msg = "Expect: " + expected.toString() + " Actual: " + actual.toString();
        assertEquals(msg, left.size(), right.size());
        for (T l : left) {
            boolean found = false;
            for (T r : right) {
                if (Objects.equal(l, r)) {
                    found = true;
                }
            }
            assertTrue(msg, found);
        }
    }
}
