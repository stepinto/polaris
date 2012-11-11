package com.codingstory.polaris.sourcedb;

import com.codingstory.polaris.parser.SourceFile;

import java.io.Closeable;
import java.io.IOException;

public interface SourceDbWriter extends Closeable {
    void writeSourceFile(SourceFile sourceFile) throws IOException;
    void writeDirectory(String project, String path) throws IOException;
    void flush() throws IOException;
    void close() throws IOException;
}
