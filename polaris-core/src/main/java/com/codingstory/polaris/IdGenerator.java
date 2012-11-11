package com.codingstory.polaris;

import java.io.IOException;

public interface IdGenerator {
    static final long MAX_RESERVED_ID = 10000;
    long next() throws IOException;
}
