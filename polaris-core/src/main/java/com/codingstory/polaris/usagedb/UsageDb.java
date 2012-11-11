package com.codingstory.polaris.usagedb;

import com.codingstory.polaris.parser.TypeUsage;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface UsageDb extends Closeable {
    List<TypeUsage> query(long typeId) throws IOException;
}
