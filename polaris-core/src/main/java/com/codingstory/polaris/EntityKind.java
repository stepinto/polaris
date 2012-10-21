package com.codingstory.polaris;

public enum EntityKind {
    ANNOTATED_SOURCE_CODE(1),
    DIRECTORY_LAYOUT(2),
    LANGUAGE_SPECIFIC_ELEMENT(3);

    private final int value;

    private EntityKind(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
