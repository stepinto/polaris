package com.codingstory.polaris.parser;

public interface VariableDeclaration extends Token {
    public String getVariableName();
    public TypeReference getTypeReference();
}
