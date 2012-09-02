package com.codingstory.polaris;

/**
 * Bypasses the exception checking system.
 */
public class SkipCheckingExceptionWrapper extends RuntimeException {
    public SkipCheckingExceptionWrapper(Throwable throwable) {
        super(throwable);
    }
}
