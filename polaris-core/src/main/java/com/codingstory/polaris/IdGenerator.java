package com.codingstory.polaris;

public interface IdGenerator {
    static final long MAX_RESERVED_ID = 10000;
    long next();
}
