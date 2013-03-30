package com.codingstory.polaris.sourcedb;

import com.codingstory.polaris.parser.ParserProtos.SourceFile;
import com.codingstory.polaris.parser.ParserProtos.FileHandle;

import java.io.Closeable;
import java.io.IOException;

public interface SourceDbWriter extends Closeable {
    void writeSourceFile(SourceFile sourceFile) throws IOException;
    void writeDirectory(FileHandle dir) throws IOException;
    void flush() throws IOException;
    void close() throws IOException;
}
