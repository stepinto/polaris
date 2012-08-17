package com.codingstory.polaris.parser;

public class PackageDeclaration implements Term {

    private final String packageName;

    public PackageDeclaration(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public Kind getKind() {
        return Kind.PACKAGE_DECLARATION;
    }

    public String getPackageName() {
        return packageName;
    }
}
