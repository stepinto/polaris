package com.codingstory.polaris.parser;

public class ClassDeclaration implements Term {

    private final String packageName;
    private final String className;

    public ClassDeclaration(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
    }

    @Override
    public Kind getKind() {
        return Kind.CLASS_DECLARATION;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }
}
