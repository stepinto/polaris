package com.codingstory.polaris.parser;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.visitor.DumpVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws Exception {
        for (String s : args) {
            process(new File(s));
        }
    }

    private static void process(File file) throws IOException, ParseException {
        CompilationUnit compilationUnit = JavaParser.parse(new FileInputStream(file));
        new DumpVisitor().visit(compilationUnit, null);
    }

}
