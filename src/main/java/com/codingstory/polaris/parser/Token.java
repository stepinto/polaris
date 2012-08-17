package com.codingstory.polaris.parser;

public interface Token {

    public static enum Kind {
        PACKAGE_DECLARATION,
        CLASS_DECLARATION,
        METHOD_DECLARATION,
        FIELD_DECLARATION,
    }

    public Kind getKind();

}
