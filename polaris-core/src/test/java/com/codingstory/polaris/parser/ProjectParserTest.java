package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.*;
import java.util.List;

import static com.codingstory.polaris.parser.TestUtils.*;
import static org.junit.Assert.*;

public class ProjectParserTest {
    private static class TestTokenCollector implements ProjectParser.TokenCollector {
        private List<Token> tokens = Lists.newArrayList();

        @Override
        public void collect(File file, Token token) {
            Preconditions.checkNotNull(file);
            Preconditions.checkNotNull(token);
            tokens.add(token);
        }

        public List<Token> getTokens() {
            return tokens;
        }
    }

    @Test
    public void testSingleFile() throws IOException {
        List<Token> tokens = parse(ImmutableList.of("package pkg; class A {}"));
        TypeDeclaration clazz = findUniqueTokenOfKind(tokens, Token.Kind.CLASS_DECLARATION);
        assertNotNull(clazz);
        assertEquals(Token.Kind.CLASS_DECLARATION, clazz.getKind());
        assertEquals(FullyQualifiedName.of("pkg.A"), clazz.getName());
    }

    @Test
    public void testResolveTypes_primitive() throws IOException {
        List<Token> tokens = parse(ImmutableList.of("package pkg; class A { int a; }"));
        FieldDeclaration field = findUniqueTokenOfKind(tokens, Token.Kind.FIELD_DECLARATION);
        assertNotNull(field);
        assertTrue(field.getTypeReferenece().isResoleved());
    }

    @Test
    public void testResolveTypes_unresolved() throws IOException {
        List<Token> tokens = parse(ImmutableList.of("package pkg; class A { B b; }"));
        FieldDeclaration field = findUniqueTokenOfKind(tokens, Token.Kind.FIELD_DECLARATION);
        assertNotNull(field);
        assertFalse(field.getTypeReferenece().isResoleved());
    }

    @Test
    public void testResolveTypes_importExplicitly() throws IOException {
        List<Token> tokens = parse(ImmutableList.of(
                "package pkg1; import pkg2.B; class A { B b; }",
                "package pkg2; class B {}; "));
        FieldDeclaration field = findUniqueTokenOfKind(tokens, Token.Kind.FIELD_DECLARATION);
        assertNotNull(field);
        assertTrue(field.getTypeReferenece().isResoleved());
        ResolvedTypeReference resolved = (ResolvedTypeReference) field.getTypeReferenece();
        assertEquals(FullyQualifiedName.of("pkg2.B"), resolved.getName());
    }

    @Test
    public void testResolveTypes_importWildcard() throws IOException {
        List<Token> tokens = parse(ImmutableList.of(
                "package pkg1; import pkg2.*; class A { B b; }",
                "package pkg2; class B {}; "));
        FieldDeclaration field = findUniqueTokenOfKind(tokens, Token.Kind.FIELD_DECLARATION);
        assertNotNull(field);
        assertTrue(field.getTypeReferenece().isResoleved());
        ResolvedTypeReference resolved = (ResolvedTypeReference) field.getTypeReferenece();
        assertEquals(FullyQualifiedName.of("pkg2.B"), resolved.getName());
    }

    @Test
    public void testResolveTypes_samePackageFirst() throws IOException {
        List<Token> tokens = parse(ImmutableList.of(
                "package pkg1; class A { B b; }",
                "package pkg1; class B {}; ",
                "package pkg2; class B {}; "));
        FieldDeclaration field = findUniqueTokenOfKind(tokens, Token.Kind.FIELD_DECLARATION);
        assertNotNull(field);
        assertTrue(field.getTypeReferenece().isResoleved());
        ResolvedTypeReference resolved = (ResolvedTypeReference) field.getTypeReferenece();
        assertEquals(FullyQualifiedName.of("pkg1.B"), resolved.getName());
    }

    // TODO: testResolvedTypes_globalPackage
    // TODO: testResolvedTypes_javaLang

    private static List<Token> parse(List<String> sources) throws IOException {
        ProjectParser parser = new ProjectParser();
        TestTokenCollector tokenCollector = new TestTokenCollector();
        parser.setProjectName("untitled");
        parser.setTokenCollector(tokenCollector);
        for (String source : sources) {
            parser.addSourceFile(createFile(source));
        }
        parser.run();
        return tokenCollector.getTokens();
    }

    private static File createFile(String content) throws IOException {
        File file = File.createTempFile("test", ".java");
        file.deleteOnExit();
        OutputStream out = new FileOutputStream(file);
        try {
            IOUtils.copy(new StringReader(content), out);
        } finally {
            IOUtils.closeQuietly(out);
        }
        return file;
    }
}
