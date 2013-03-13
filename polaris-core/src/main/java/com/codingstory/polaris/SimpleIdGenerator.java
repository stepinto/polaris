package com.codingstory.polaris;

import java.io.IOException;

public class SimpleIdGenerator implements IdGenerator {
    private long n = MAX_RESERVED_ID + 1;

    @Override
    public long next() {
        return n++;
    }
}
