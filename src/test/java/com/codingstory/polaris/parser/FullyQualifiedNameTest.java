package com.codingstory.polaris.parser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FullyQualifiedNameTest {
    @Test
    public void testConstruction1() {
        FullyQualifiedName n = FullyQualifiedName.of("com.codingstory.polaris.MyType");
        assertEquals("com.codingstory.polaris", n.getPackageName());
        assertEquals("MyType", n.getTypeName());
    }

    @Test
    public void testConstuction2() {
        FullyQualifiedName n = FullyQualifiedName.of("com.codingstory.polaris", "MyType");
        assertEquals("com.codingstory.polaris", n.getPackageName());
        assertEquals("MyType", n.getTypeName());
    }

    @Test
    public void testEqualsAndHashCode() {
        FullyQualifiedName a = FullyQualifiedName.of("com.codingstory.polaris", "MyType");
        FullyQualifiedName b = FullyQualifiedName.of("com.codingstory.polaris.MyType");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testToString() {
        FullyQualifiedName n = FullyQualifiedName.of("com.codingstory.polaris", "MyType");
        assertEquals("com.codingstory.polaris.MyType", n.toString());
    }
}
