package com.codingstory.polaris.parser;

public interface TypeDeclaration extends Token {

    public String getPackageName();

    /**
     * @return the name of a class, interface or enum
     */
    public String getTypeName();

}
