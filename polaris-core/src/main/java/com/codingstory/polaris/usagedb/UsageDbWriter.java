package com.codingstory.polaris.usagedb;

import com.codingstory.polaris.parser.TypeUsage;

import java.io.Closeable;
import java.io.IOException;

public interface UsageDbWriter extends Closeable {
    void write(TypeUsage usage) throws IOException;
    void flush() throws IOException;
}
