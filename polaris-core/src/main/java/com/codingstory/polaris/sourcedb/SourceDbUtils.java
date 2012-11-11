package com.codingstory.polaris.sourcedb;

public class SourceDbUtils {
    public static String fixPathForDirectory(String path) {
        if (path.endsWith("/")) {
            return path;
        } else {
            return path + "/";
        }
    }

    public static boolean isDirectory(String path) {
        return path.endsWith("/");
    }
}
