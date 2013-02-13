package com.codingstory.polaris.usagedb;

import com.codingstory.polaris.parser.ParserProtos.Usage;

import java.io.Closeable;
import java.io.IOException;

public interface UsageDbWriter extends Closeable {
    void write(Usage usage) throws IOException;
    void flush() throws IOException;
}
