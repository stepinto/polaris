package com.codingstory.polaris.parser;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.junit.Test;

import java.io.*;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JavaTermExtractorTest {

    @Test
    public void testPackage() throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("package my_package;");
        pw.println("public class MyClass {");
        pw.println("}");
        pw.close();
        String code = sw.toString();

        List<Term> terms = new JavaTermExtractor()
                .setInputStream(new ByteArrayInputStream(code.getBytes()))
                .extractTerms();
        PackageDeclaration pkg = findUniqueTermOfKind(terms, Term.Kind.PACKAGE_DECLARATION);
        assertEquals(Term.Kind.PACKAGE_DECLARATION, pkg.getKind());
        assertEquals("my_package", pkg.getPackageName());
    }

    private static <T extends Term> T findUniqueTermOfKind(List<Term> terms, final Term.Kind kind) {
        return (T) Iterables.getOnlyElement(Iterables.filter(terms, new Predicate<Term>() {
            @Override
            public boolean apply(Term term) {
                return term.getKind() == kind;
            }
        }));
    }

}
