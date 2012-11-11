package com.codingstory.polaris.typedb;

import com.codingstory.polaris.parser.ClassType;

import java.io.Closeable;
import java.io.IOException;

public interface TypeDbWriter extends Closeable {
    void write(ClassType type) throws IOException;
    void flush() throws IOException;
}
