package com.codingstory.polaris.sourcedb;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.SimpleIdGenerator;
import com.codingstory.polaris.parser.ParserProtos.FileHandle;
import com.codingstory.polaris.parser.ParserProtos.SourceFile;
import com.codingstory.polaris.search.SearchProtos.Hit;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.codingstory.polaris.TestUtils.assertEqualsIgnoreOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SourceDbTest {
    private static final String TEST_PROJECT = "TestProject";
    private static final IdGenerator ID_GENERATOR = new SimpleIdGenerator();
    private File tempDir;

    @Before
    public void setUp() {
        tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
    }

    @Test
    public void testSource() throws IOException {
        SourceDbWriter w = new SourceDbWriterImpl(tempDir);
        long f1;
        long f2;
        try {
            f1 = writeFile(w, "/dir/a", "hello");
            f2 = writeFile(w, "/dir/b", "world");
            w.flush();
        } finally {
            w.close();
        }

        SourceDb r = new SourceDbImpl(tempDir);
        try {
            assertEquals("hello", r.querySourceById(f1).getSource());
            assertEquals("hello", r.querySourceByPath(TEST_PROJECT, "/dir/a").getSource());
            assertEquals("world", r.querySourceById(f2).getSource());
            assertEquals("world", r.querySourceByPath(TEST_PROJECT, "/dir/b").getSource());
            assertNull(r.querySourceById(3L));
            assertNull(r.querySourceByPath("NoSuchProject", "/dir/a"));
            assertNull(r.querySourceByPath(TEST_PROJECT, "/nosuchfile"));
        } finally  {
            r.close();
        }
    }

    @Test
    public void testListDirectory() throws IOException {
        SourceDbWriter w = new SourceDbWriterImpl(tempDir);
        try {
            writeFile(w, "/dir/a", "hello");
            writeFile(w, "/dir/b", "world");
            writeDirectory(w, "/dir/c/");
            writeDirectory(w, "/dir/");
            w.flush();
        } finally {
            w.close();
        }

        SourceDb r = new SourceDbImpl(tempDir);
        try {
            r.listDirectory(TEST_PROJECT, "/");
            assertEqualsIgnoreOrder(
                    ImmutableList.of("/dir/"),
                    listFiles(r, "/"));
            assertEqualsIgnoreOrder(
                    ImmutableList.of("/dir/a", "/dir/b", "/dir/c/"),
                    listFiles(r, "/dir/"));
            assertEqualsIgnoreOrder(
                    ImmutableList.<String>of(),
                    listFiles(r, "/dir/c/"));
        } finally {
            r.close();
        }

        // TODO: We cannot distinguish whether the dir does not exist or it has no children yet.
        // assertNull(r.listDirectory("NoSuchProject", "/"));
        // assertNull(listFiles(r, "/NoSuchDir"));
    }

    @Test
    public void testQuery_fileName() throws IOException {
        SourceDbWriter w = new SourceDbWriterImpl(tempDir);
        try {
            writeFile(w, "/dir1/a", "hello");
            writeFile(w, "/dir1/b", "world");
            writeFile(w, "/dir2/a", "hello");
            writeFile(w, "/dir2/b", "world");
            w.flush();
        } finally {
            w.close();
        }

        SourceDb r = new SourceDbImpl(tempDir);
        try {
            List<Hit> hits;
            Hit hit;

            // full path
            hits = r.query("/dir1/a", 10);
            assertFalse(hits.isEmpty());
            hit = hits.get(0);
            assertEquals(Hit.Kind.FILE, hit.getKind());
            assertEquals("/dir1/a", hit.getJumpTarget().getFile().getPath());

            // partial path
            hits = r.query("dir1", 10);
            assertEquals(2, hits.size());
            hits = r.query("b", 10);
            assertEquals(2, hits.size());
            hits = r.query("dir1 a", 10);
            assertEquals(3, hits.size());
        } finally {
            r.close();
        }
    }

    @Test
    public void testQuery_content() throws IOException {
        SourceDbWriter w = new SourceDbWriterImpl(tempDir);
        try {
        writeFile(w, "/1", "hello");
        writeFile(w, "/2", "world");
        writeFile(w, "/3", "hello world");
        } finally  {
            w.close();
        }

        SourceDb r = new SourceDbImpl(tempDir);
        try {
            List<Hit> hits = r.query("hello", 10);
            assertEquals(2, hits.size());
        } finally {
            r.close();
        }
    }

    @Test
    public void testGetFileHandle() throws IOException {
        SourceDbWriter w = new SourceDbWriterImpl(tempDir);
        long a;
        long b;
        try {
            a = writeFile(w, "/a", "a");
            b = writeDirectory(w, "/b/");
        } finally {
            w.close();
        }

        SourceDb r = new SourceDbImpl(tempDir);
        try {
            assertEquals(a, r.getFileHandle(TEST_PROJECT, "/a").getId());
            assertEquals(b, r.getFileHandle(TEST_PROJECT, "/b/").getId());
            assertNull(r.getFileHandle("NoSuchProject", "/"));
            assertNull(r.getFileHandle(TEST_PROJECT, "/NoSuchFile"));
        } finally {
            r.close();
        }
    }

    private long writeFile(SourceDbWriter w, String path, String content) throws IOException {
        long fileId = ID_GENERATOR.next();
        FileHandle f = FileHandle.newBuilder()
                .setKind(FileHandle.Kind.NORMAL_FILE)
                .setId(fileId)
                .setProject(TEST_PROJECT)
                .setPath(path)
                .build();
        SourceFile source = SourceFile.newBuilder()
                .setHandle(f)
                .setSource(content)
                .setAnnotatedSource(content)
                .build();
        w.writeSourceFile(source);
        return fileId;
    }

    private long writeDirectory(SourceDbWriter w, String path) throws IOException {
        long fileId = ID_GENERATOR.next();
        FileHandle f = FileHandle.newBuilder()
                .setKind(FileHandle.Kind.DIRECTORY)
                .setId(fileId)
                .setProject(TEST_PROJECT)
                .setPath(path)
                .build();
        w.writeDirectory(f);
        return fileId;
    }

    private List<String> listFiles(SourceDb r, String path) throws IOException {
        List<String> results = Lists.newArrayList();
        List<FileHandle> fileHandles = r.listDirectory(TEST_PROJECT, path);
        if (fileHandles == null) {
            return null;
        }
        for (FileHandle fileHandle : fileHandles) {
            if (fileHandle.getKind() == FileHandle.Kind.DIRECTORY) {
                assertTrue(fileHandle.getPath().endsWith("/"));
            } else {
                assertFalse(fileHandle.getPath().endsWith("/"));
            }
            results.add(fileHandle.getPath());
        }
        return results;
    }
}
