package com.codingstory.polaris.usagedb;

import com.codingstory.polaris.parser.FullTypeName;
import com.codingstory.polaris.parser.Position;
import com.codingstory.polaris.parser.Span;
import com.codingstory.polaris.parser.TypeHandle;
import com.codingstory.polaris.parser.TypeUsage;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class UsageDbTest {
    private static final String TEST_PROJECT = "TestProject";
    private File tempDir;

    @Before
    public void setUp() {
        tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
    }

    @Test
    public void testSimple() throws IOException {
        UsageDbWriter w = new UsageDbWriterImpl(tempDir);
        long typeId = 100;
        TypeHandle handle = new TypeHandle(typeId, FullTypeName.of("MyClass"));
        TypeUsage usage1 = new TypeUsage(
                handle, new Span(new Position(0, 10), new Position(0, 20)), TypeUsage.Kind.METHOD_SIGNATURE);
        TypeUsage usage2 = new TypeUsage(
                handle, new Span(new Position(0, 20), new Position(0, 30)), TypeUsage.Kind.METHOD_SIGNATURE);
        w.write(usage1);
        w.write(usage2);
        w.close();
        UsageDb r = new UsageDbImpl(tempDir);
        List<TypeUsage> usages = Lists.newArrayList(r.query(typeId));
        assertEquals(2, usages.size());
        Collections.sort(usages, new Comparator<TypeUsage>() {
            @Override
            public int compare(TypeUsage left, TypeUsage right) {
                return left.getSpan().compareTo(right.getSpan());
            }
        });
        assertEquals(2, usages.size());
        assertEquals(usage1.getSpan(), usages.get(0).getSpan());
        assertEquals(usage2.getSpan(), usages.get(1).getSpan());
        r.close();
    }
}
