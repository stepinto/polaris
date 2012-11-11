package com.codingstory.polaris.parser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FullTypeNameTest {
    @Test
    public void testConstruction1() {
        FullTypeName n = FullTypeName.of("com.codingstory.polaris.MyType");
        assertEquals("com.codingstory.polaris", n.getPackageName());
        assertEquals("MyType", n.getTypeName());
    }

    @Test
    public void testConstruction2() {
        FullTypeName n = FullTypeName.of("com.codingstory.polaris", "MyType");
        assertEquals("com.codingstory.polaris", n.getPackageName());
        assertEquals("MyType", n.getTypeName());
    }

    @Test
    public void testEqualsAndHashCode() {
        FullTypeName a = FullTypeName.of("com.codingstory.polaris", "MyType");
        FullTypeName b = FullTypeName.of("com.codingstory.polaris.MyType");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testToString() {
        FullTypeName n = FullTypeName.of("com.codingstory.polaris", "MyType");
        assertEquals("com.codingstory.polaris.MyType", n.toString());
    }
}
