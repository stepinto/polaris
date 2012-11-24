package com.codingstory.polaris.typedb;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.JumpTarget;
import com.codingstory.polaris.SimpleIdGenerator;
import com.codingstory.polaris.parser.ClassType;
import com.codingstory.polaris.parser.Field;
import com.codingstory.polaris.parser.FullTypeName;
import com.codingstory.polaris.parser.Method;
import com.codingstory.polaris.parser.Modifier;
import com.codingstory.polaris.parser.TypeHandle;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TypeDbTest {
    private static final IdGenerator ID_GENERATOR = new SimpleIdGenerator();
    private File tempDir;

    @Before
    public void setUp() {
        tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
    }

    private static class ClassTypeFactory {
        static final long FAKE_FILE_ID = 100L;
        public static ClassType create(FullTypeName type) throws IOException {
            return createInFile(type, FAKE_FILE_ID);
        }

        public static ClassType createInFile(FullTypeName type, long fileId) throws IOException {
            return new ClassType(new TypeHandle(ID_GENERATOR.next(), type),
                    ClassType.Kind.CLASS,
                    Lists.<TypeHandle>newArrayList(),
                    EnumSet.noneOf(Modifier.class),
                    Lists.<Field>newArrayList(),
                    Lists.<Method>newArrayList(),
                    null,
                    new JumpTarget(fileId, 0));
        }
    }

    @Test
    public void testQueryExact() throws IOException {
        List<ClassType> types = Lists.newArrayList();
        int n = 10;
        for (int i = 0; i < n; i++) {
            types.add(ClassTypeFactory.create(FullTypeName.of("MyClass" + i)));
        }
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        for (ClassType t : types) {
            w.write(t);
        }
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        for (ClassType type : types) {
            ClassType result = r.queryByTypeId(type.getHandle().getId());
            assertNotNull(result);
            assertEquals(type.getHandle(), result.getHandle());
        }
        r.close();
    }

    @Test
    public void testQueryByTypeName() throws IOException {
        List<ClassType> types = Lists.newArrayList();
        int n = 10;
        for (int i = 0; i < n; i++) {
            types.add(ClassTypeFactory.create(FullTypeName.of("MyClass")));
        }
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        for (ClassType t : types) {
            w.write(t);
        }
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        List<ClassType> result = Lists.newArrayList(r.queryByTypeName(FullTypeName.of("MyClass")));
        assertEquals(n, result.size());
        r.close();
    }

    @Test
    public void testTypeNotFound() throws IOException {
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        assertNull(r.queryByTypeId(Integer.MAX_VALUE));
        r.close();
    }

    @Test
    public void testQueryForAutoCompletion_fullType() throws IOException {
        List<String> corpus = ImmutableList.of("java.util.ArrayList", "java.util.List", "java.util.LinkedList");
        doTestQueryForAutoCompletion("java.util.list", corpus, ImmutableList.of("java.util.List"));
        doTestQueryForAutoCompletion("java.util.l", corpus, ImmutableList.of("java.util.List", "java.util.LinkedList"));
        doTestQueryForAutoCompletion("java.util.lin", corpus, ImmutableList.of("java.util.LinkedList"));
    }

    @Test
    public void testQueryForAutoCompletion_simpleType() throws IOException {
        List<String> corpus = ImmutableList.of("java.util.concurrent.Future", "java.io.File", "java.io.FileReader");
        doTestQueryForAutoCompletion("Future", corpus, ImmutableList.of("java.util.concurrent.Future"));
        doTestQueryForAutoCompletion("Fi", corpus, ImmutableList.of("java.io.File", "java.io.FileReader"));
    }

    @Test
    public void testQueryForAutoCompletion_acronym() throws IOException {
        List<String> corpus = ImmutableList.of("java.io.InputStream", "java.io.FileInputStream",
                "java.io.InputStreamReader", "java.io.Reader", "java.io.FileReader");
        doTestQueryForAutoCompletion("FIS", corpus, ImmutableList.of("java.io.FileInputStream"));
        doTestQueryForAutoCompletion("FR", corpus, ImmutableList.of("java.io.FileReader"));
        doTestQueryForAutoCompletion("F", corpus, ImmutableList.of("java.io.FileInputStream", "java.io.FileReader"));
    }

    @Test
    public void testQueryInFile() throws IOException {
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        w.write(ClassTypeFactory.createInFile(FullTypeName.of("A"), 1000L));
        w.write(ClassTypeFactory.createInFile(FullTypeName.of("B"), 1000L));
        w.write(ClassTypeFactory.createInFile(FullTypeName.of("C"), 1001L));
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        List<ClassType> types = r.queryInFile(1000L, Integer.MAX_VALUE);
        assertEquals(ImmutableSet.of(FullTypeName.of("A"), FullTypeName.of("B")),
                ImmutableSet.copyOf(getFullTypeNames(types)));
    }

    private void doTestQueryForAutoCompletion(String query,
            Iterable<String> corpus, Iterable<String> expected) throws IOException {
        FileUtils.cleanDirectory(tempDir);
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        for (String type : corpus) {
            w.write(ClassTypeFactory.create(FullTypeName.of(type)));
        }
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        List<ClassType> result = r.queryForAutoCompletion(query, Integer.MAX_VALUE);
        List<FullTypeName> expectedTypes = Lists.newArrayList();
        for (String s : expected) {
            expectedTypes.add(FullTypeName.of(s));
        }
        assertEquals(ImmutableSet.copyOf(expectedTypes), ImmutableSet.copyOf(getFullTypeNames(result)));
    }

    private static List<FullTypeName> getFullTypeNames(List<ClassType> classTypes) {
        List<FullTypeName> result = Lists.newArrayList();
        for (ClassType classType : classTypes) {
            result.add(classType.getName());
        }
        return result;
    }
}
