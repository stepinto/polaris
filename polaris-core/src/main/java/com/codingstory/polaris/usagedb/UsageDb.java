package com.codingstory.polaris.usagedb;

import com.codingstory.polaris.parser.ParserProtos.Usage;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface UsageDb extends Closeable {
    List<Usage> query(Usage.Kind kind, long id) throws IOException;
}
