package com.codingstory.polaris.cli;

import com.codingstory.polaris.search.SearchProtos.StatusCode;
import com.google.common.base.Objects;

import java.io.File;

public final class CommandUtils {
    private CommandUtils() {}

    public static void die(String message) {
        System.err.println("polaris: " + message);
        System.exit(1);
    }

    public static void checkStatus(StatusCode statusCode) {
        if (!Objects.equal(StatusCode.OK, statusCode)) {
            die("Server error: " + statusCode.name() + "(" + statusCode.ordinal() + ")");
        }
    }

    public static void checkDirectoryExists(File dir) {
        if (!dir.isDirectory()) {
            die(dir + " does not exist");
        }
    }
}
