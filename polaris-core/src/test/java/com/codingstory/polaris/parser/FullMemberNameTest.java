package com.codingstory.polaris.parser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FullMemberNameTest {
    @Test
    public void testConstruction1() {
        FullMemberName n = FullMemberName.of("pkg.A#m");
        assertEquals(FullTypeName.of("pkg.A"), n.getFullTypeName());
        assertEquals("m", n.getMemberName());
    }

    @Test
    public void testConstruction2() {
        FullMemberName n = FullMemberName.of("A#m");
        assertEquals(FullTypeName.of("A"), n.getFullTypeName());
        assertEquals("m", n.getMemberName());
    }

    @Test
    public void testConstruction3() {
        FullMemberName n = FullMemberName.of("A#<static>");
        assertEquals(FullTypeName.of("A"), n.getFullTypeName());
        assertEquals("<static>", n.getMemberName());
    }

    @Test
    public void testEqualsAndHashCode() {
        FullMemberName a = FullMemberName.of(FullTypeName.of("pkg", "A"), "m");
        FullMemberName b = FullMemberName.of("pkg.A#m");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testToString() {
        FullMemberName n = FullMemberName.of("pkg.A#m");
        assertEquals("pkg.A#m", n.toString());
    }
}
