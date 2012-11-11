package com.codingstory.polaris;

public class IdUtils {
    private IdUtils() {}

    public static long checkValid(long id) {
        if (!isValid(id)) {
            throw new IllegalArgumentException("Invalid id: " + id);
        }
        return id;
    }

    public static boolean isValid(long id) {
        return id > 0;
    }
}
