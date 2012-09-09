package com.codingstory.polaris.parser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FullyQualifiedTypeNameTest {
    @Test
    public void testConstruction1() {
        FullyQualifiedTypeName n = FullyQualifiedTypeName.of("com.codingstory.polaris.MyType");
        assertEquals("com.codingstory.polaris", n.getPackageName());
        assertEquals("MyType", n.getTypeName());
    }

    @Test
    public void testConstuction2() {
        FullyQualifiedTypeName n = FullyQualifiedTypeName.of("com.codingstory.polaris", "MyType");
        assertEquals("com.codingstory.polaris", n.getPackageName());
        assertEquals("MyType", n.getTypeName());
    }

    @Test
    public void testEqualsAndHashCode() {
        FullyQualifiedTypeName a = FullyQualifiedTypeName.of("com.codingstory.polaris", "MyType");
        FullyQualifiedTypeName b = FullyQualifiedTypeName.of("com.codingstory.polaris.MyType");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testToString() {
        FullyQualifiedTypeName n = FullyQualifiedTypeName.of("com.codingstory.polaris", "MyType");
        assertEquals("com.codingstory.polaris.MyType", n.toString());
    }
}
