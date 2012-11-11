package com.codingstory.polaris.sourcedb;

import com.codingstory.polaris.parser.SourceFile;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SourceDbTest {
    private static final String TEST_PROJECT = "TestProject";
    private File tempDir;

    @Before
    public void setUp() {
        tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
    }

    @Test
    public void testSource() throws IOException {
        SourceDbWriter w = new SourceDbWriterImpl(tempDir);
        w.writeSourceFile(new SourceFile(1L, TEST_PROJECT, "/dir/a", "a", "a"));
        w.writeSourceFile(new SourceFile(2L, TEST_PROJECT, "/dir/b", "b", "b"));
        w.close();

        SourceDb r = new SourceDbImpl(tempDir);
        assertEquals("a", r.querySourceById(1L).getSource());
        assertEquals("a", r.querySourceByPath(TEST_PROJECT, "/dir/a").getSource());
        assertEquals("b", r.querySourceById(2L).getSource());
        assertEquals("b", r.querySourceByPath(TEST_PROJECT, "/dir/b").getSource());
        assertNull(r.querySourceById(3L));
        assertNull(r.querySourceByPath("NoSuchProject", "/dir/a"));
        assertNull(r.querySourceByPath(TEST_PROJECT, "/nosuchfile"));
    }

    @Test
    public void testListDirectory() throws IOException {
        SourceDbWriter w = new SourceDbWriterImpl(tempDir);
        w.writeSourceFile(new SourceFile(1L, TEST_PROJECT, "/dir/a", "a", "a"));
        w.writeSourceFile(new SourceFile(2L, TEST_PROJECT, "/dir/b", "b", "b"));
        w.writeDirectory(TEST_PROJECT, "/dir/c");
        w.writeDirectory(TEST_PROJECT, "/dir");
        w.close();

        SourceDb r = new SourceDbImpl(tempDir);
        assertEquals(ImmutableList.of("/dir/"), r.listDirectory(TEST_PROJECT, "/"));
        assertEquals(ImmutableSet.of("/dir/a", "/dir/b", "/dir/c/"),
                ImmutableSet.copyOf(r.listDirectory(TEST_PROJECT, "/dir")));
        // TODO: Currently we can't tell non-existant directory or empty directory.
        // assertNull(r.listDirectory("NoSuchProject", "/"));
        // assertNull(r.listDirectory(TEST_PROJECT, "/nosuchdir"));
    }
}
