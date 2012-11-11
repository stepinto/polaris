package com.codingstory.polaris;

/**
 * Generates id for static variables, like type IDs of {@code PrimitiveType}s.
 */
public class ReservedIdGenerator {
    private static int n = 1;

    public static long generateReservedId() {
        if (n >= IdGenerator.MAX_RESERVED_ID) {
            throw new AssertionError("Exceeding MAX_RESERVED_ID");
        }
        return n++;
    }
}
