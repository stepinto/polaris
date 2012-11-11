package com.codingstory.polaris.indexing;

import java.io.File;

public class IndexPathUtils {
    private IndexPathUtils() {}

    public static File getTypeDbPath(File base) {
        return new File(base, "typedb");
    }

    public static File getUsageDbPath(File base) {
        return new File(base, "usagedb");
    }

    public static File getSourceDbPath(File base) {
        return new File(base, "sourcedb");
    }
}
