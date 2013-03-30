package com.codingstory.polaris.usagedb;

import com.codingstory.polaris.parser.ParserProtos.ClassTypeHandle;
import com.codingstory.polaris.parser.ParserProtos.FileHandle;
import com.codingstory.polaris.parser.ParserProtos.JumpTarget;
import com.codingstory.polaris.parser.ParserProtos.TypeUsage;
import com.codingstory.polaris.parser.ParserProtos.Usage;
import com.codingstory.polaris.parser.TypeUtils;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.codingstory.polaris.parser.TypeUtils.handleOf;
import static com.codingstory.polaris.parser.TypeUtils.positionOf;
import static com.codingstory.polaris.parser.TypeUtils.spanOf;
import static com.codingstory.polaris.parser.TypeUtils.usageOf;
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
        long typeId = 100L;
        long fileId = 200L;
        FileHandle file = FileHandle.newBuilder()
                .setKind(FileHandle.Kind.NORMAL_FILE)
                .setId(fileId)
                .setPath("project")
                .setPath("/filename")
                .build();
        ClassTypeHandle clazz = ClassTypeHandle.newBuilder()
                .setId(typeId)
                .setName("MyClass")
                .setResolved(true)
                .build();
        JumpTarget jumpTarget1 = JumpTarget.newBuilder()
                .setFile(file)
                .setSpan(spanOf(positionOf(0, 10), positionOf(0, 20)))
                .build();
        Usage usage1 = usageOf(TypeUsage.newBuilder()
                .setType(handleOf(clazz))
                .setKind(TypeUsage.Kind.METHOD_SIGNATURE)
                .build(), jumpTarget1, jumpTarget1, "snippet");
        JumpTarget jumpTarget2 = JumpTarget.newBuilder()
                .setFile(file)
                .setSpan(spanOf(positionOf(0, 20), positionOf(0, 30)))
                .build();
        Usage usage2 = usageOf(TypeUsage.newBuilder()
                .setType(handleOf(clazz))
                .setKind(TypeUsage.Kind.METHOD_SIGNATURE)
                .build(), jumpTarget2, jumpTarget2, "snippet");
        w.write(usage1);
        w.write(usage2);
        w.close();
        UsageDb r = new UsageDbImpl(tempDir);
        List<Usage> usages = Lists.newArrayList(r.query(Usage.Kind.TYPE, typeId));
        assertEquals(2, usages.size());
        Collections.sort(usages, new Comparator<Usage>() {
            @Override
            public int compare(Usage left, Usage right) {
                return TypeUtils.SPAN_COMPARATOR.compare(
                        left.getJumpTarget().getSpan(), right.getJumpTarget().getSpan());
            }
        });
        assertEquals(2, usages.size());
        assertEquals(usage1.getJumpTarget(), usages.get(0).getJumpTarget());
        assertEquals(usage2.getJumpTarget(), usages.get(1).getJumpTarget());
        r.close();
    }
}
