package com.codingstory.polaris.parser;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.io.input.NullInputStream;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class JavaTokenExtractorTest {

    @Test
    public void testEmpty() throws IOException {
        List<Token> tokens = new JavaTokenExtractor()
                .setInputStream(new NullInputStream(0))
                .extractTerms();
        assertTrue(tokens.isEmpty());
    }

    /*
    @Test
    public void testPackage() throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("package com.example.package;");
        pw.println("public class MyClass {");
        pw.println("}");
        pw.close();
        String code = sw.toString();

        List<Token> tokens = new JavaTokenExtractor()
                .setInputStream(new ByteArrayInputStream(code.getBytes()))
                .extractTerms();
        PackageDeclaration pkg = findUniqueTermOfKind(tokens, Token.Kind.PACKAGE_DECLARATION);
        assertEquals(Token.Kind.PACKAGE_DECLARATION, pkg.getKind());
        assertEquals("com.example.package", pkg.getPackageName());
    }
    */

    private static <T extends Token> T findUniqueTermOfKind(List<Token> tokens, final Token.Kind kind) {
        return (T) Iterables.getOnlyElement(Iterables.filter(tokens, new Predicate<Token>() {
            @Override
            public boolean apply(Token token) {
                return token.getKind() == kind;
            }
        }));
    }

}
