package com.codingstory.polaris.typedb;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.JumpTarget;
import com.codingstory.polaris.SimpleIdGenerator;
import com.codingstory.polaris.parser.ClassType;
import com.codingstory.polaris.parser.Field;
import com.codingstory.polaris.parser.FieldHandle;
import com.codingstory.polaris.parser.FullMemberName;
import com.codingstory.polaris.parser.FullTypeName;
import com.codingstory.polaris.parser.Method;
import com.codingstory.polaris.parser.MethodHandle;
import com.codingstory.polaris.parser.Modifier;
import com.codingstory.polaris.parser.Position;
import com.codingstory.polaris.parser.PrimitiveType;
import com.codingstory.polaris.parser.TypeHandle;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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
    static final long FAKE_FILE_ID = 100L;
    private File tempDir;

    @Before
    public void setUp() {
        tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
    }

    @Test
    public void testGetTypeById() throws IOException {
        List<ClassType> types = Lists.newArrayList();
        int n = 10;
        for (int i = 0; i < n; i++) {
            types.add(createEmptyClass(FullTypeName.of("MyClass" + i)));
        }
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        for (ClassType t : types) {
            w.write(t);
        }
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        for (ClassType type : types) {
            ClassType result = r.getTypeById(type.getHandle().getId());
            assertNotNull(result);
            assertEquals(type.getHandle(), result.getHandle());
        }
        r.close();
    }

    @Test
    public void testGetTypeById_notFound() throws IOException {
        new TypeDbWriterImpl(tempDir).close();
        TypeDb r = new TypeDbImpl(tempDir);
        assertNull(r.getTypeById(Integer.MAX_VALUE));
        r.close();
    }

    @Test
    public void testGetTypeByName() throws IOException {
        List<ClassType> types = Lists.newArrayList();
        int n = 10;
        for (int i = 0; i < n; i++) {
            types.add(createEmptyClass(FullTypeName.of("MyClass")));
        }
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        for (ClassType t : types) {
            w.write(t);
        }
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        List<ClassType> result = Lists.newArrayList(r.getTypeByName(FullTypeName.of("MyClass")));
        assertEquals(n, result.size());
        r.close();
    }

    @Test
    public void testCompleteQuery_fullType() throws IOException {
        List<String> corpus = ImmutableList.of("java.util.ArrayList", "java.util.List", "java.util.LinkedList");
        doTestCompleteQuery("java.util.list", corpus, ImmutableList.of("java.util.List"));
        doTestCompleteQuery("java.util.l", corpus, ImmutableList.of("java.util.List", "java.util.LinkedList"));
        doTestCompleteQuery("java.util.lin", corpus, ImmutableList.of("java.util.LinkedList"));
    }

    @Test
    public void testCompleteQuery_simpleType() throws IOException {
        List<String> corpus = ImmutableList.of("java.util.concurrent.Future", "java.io.File", "java.io.FileReader");
        doTestCompleteQuery("Future", corpus, ImmutableList.of("java.util.concurrent.Future"));
        doTestCompleteQuery("Fi", corpus, ImmutableList.of("java.io.File", "java.io.FileReader"));
    }

    @Test
    public void testCompleteQuery_acronym() throws IOException {
        List<String> corpus = ImmutableList.of("java.io.InputStream", "java.io.FileInputStream",
                "java.io.InputStreamReader", "java.io.Reader", "java.io.FileReader");
        doTestCompleteQuery("FIS", corpus, ImmutableList.of("java.io.FileInputStream"));
        doTestCompleteQuery("FR", corpus, ImmutableList.of("java.io.FileReader"));
        doTestCompleteQuery("F", corpus, ImmutableList.of("java.io.FileInputStream", "java.io.FileReader"));
    }

    @Test
    public void testQueryInFile() throws IOException {
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        w.write(createClassInFile(FullTypeName.of("A"), 1000L));
        w.write(createClassInFile(FullTypeName.of("B"), 1000L));
        w.write(createClassInFile(FullTypeName.of("C"), 1001L));
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        List<ClassType> types = r.getTypesInFile(1000L, Integer.MAX_VALUE);
        assertEquals(ImmutableSet.of(FullTypeName.of("A"), FullTypeName.of("B")),
                ImmutableSet.copyOf(getFullTypeNames(types)));
    }

    private void doTestCompleteQuery(String query,
            Iterable<String> corpus, Iterable<String> expected) throws IOException {
        FileUtils.cleanDirectory(tempDir);
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        for (String type : corpus) {
            w.write(createEmptyClass(FullTypeName.of(type)));
        }
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        List<ClassType> result = r.completeQuery(query, Integer.MAX_VALUE);
        List<FullTypeName> expectedTypes = Lists.newArrayList();
        for (String s : expected) {
            expectedTypes.add(FullTypeName.of(s));
        }
        assertEquals(ImmutableSet.copyOf(expectedTypes), ImmutableSet.copyOf(getFullTypeNames(result)));
    }

    @Test
    public void testGetFieldById() throws IOException {
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        int n = 0;
        List<ClassType> types = Lists.newArrayList();
        for (int i = 0; i < n; i++) {
            ClassType type = createClassWithOneField(FullTypeName.of("MyClass" + i), "myField");
            types.add(type);
            w.write(type);
        }
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        for (ClassType type : types) {
            Field f = r.getFieldById(Iterables.getOnlyElement(type.getFields()).getHandle().getId());
            assertNotNull(f);
            assertEquals(FullMemberName.of(type.getName(), "myField"), f.getName());
        }
        assertNull(r.getFieldById(Long.MAX_VALUE));
        r.close();
    }

    @Test
    public void testGetMethodById() throws IOException {
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        int n = 0;
        List<ClassType> types = Lists.newArrayList();
        for (int i = 0; i < n; i++) {
            ClassType type = createClassWithOneMethod(FullTypeName.of("MyClass" + i), "myMethod");
            types.add(type);
            w.write(type);
        }
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        for (ClassType type : types) {
            Method m = r.getMethodById(Iterables.getOnlyElement(type.getMethods()).getHandle().getId());
            assertNotNull(m);
            assertEquals(FullMemberName.of(type.getName(), "myMethod"), m.getName());
        }
        assertNull(r.getFieldById(Long.MAX_VALUE));
        r.close();
    }

    private static List<FullTypeName> getFullTypeNames(List<ClassType> classTypes) {
        List<FullTypeName> result = Lists.newArrayList();
        for (ClassType classType : classTypes) {
            result.add(classType.getName());
        }
        return result;
    }

    private static ClassType createEmptyClass(FullTypeName type) throws IOException {
        return new ClassType(new TypeHandle(ID_GENERATOR.next(), type),
                ClassType.Kind.CLASS,
                Lists.<TypeHandle>newArrayList(),
                EnumSet.noneOf(Modifier.class),
                Lists.<Field>newArrayList(),
                Lists.<Method>newArrayList(),
                null,
                new JumpTarget(FAKE_FILE_ID, Position.ZERO));
    }

    private static ClassType createClassInFile(FullTypeName type, long fileId) throws IOException {
        return new ClassType(new TypeHandle(ID_GENERATOR.next(), type),
                ClassType.Kind.CLASS,
                Lists.<TypeHandle>newArrayList(),
                EnumSet.noneOf(Modifier.class),
                Lists.<Field>newArrayList(),
                Lists.<Method>newArrayList(),
                null,
                new JumpTarget(fileId, Position.ZERO));
    }

    private ClassType createClassWithOneField(FullTypeName type, String fieldName) throws IOException {
        Field field = new Field(
                new FieldHandle(ID_GENERATOR.next(), FullMemberName.of(type, fieldName)),
                PrimitiveType.INTEGER.getHandle(),
                EnumSet.noneOf(Modifier.class),
                new JumpTarget(FAKE_FILE_ID, Position.ZERO));
        return new ClassType(new TypeHandle(ID_GENERATOR.next(), type),
                ClassType.Kind.CLASS,
                Lists.<TypeHandle>newArrayList(),
                EnumSet.noneOf(Modifier.class),
                ImmutableList.of(field),
                Lists.<Method>newArrayList(),
                null,
                new JumpTarget(FAKE_FILE_ID, Position.ZERO));
    }

    private ClassType createClassWithOneMethod(FullTypeName type, String methodName) throws IOException {
        Method method= new Method(
                new MethodHandle(
                        ID_GENERATOR.next(), FullMemberName.of(type, methodName), ImmutableList.<TypeHandle>of()),
                PrimitiveType.INTEGER.getHandle(),
                ImmutableList.<Method.Parameter>of(),
                ImmutableList.<TypeHandle>of(),
                EnumSet.noneOf(Modifier.class),
                new JumpTarget(FAKE_FILE_ID, Position.ZERO));
        return new ClassType(new TypeHandle(ID_GENERATOR.next(), type),
                ClassType.Kind.CLASS,
                Lists.<TypeHandle>newArrayList(),
                EnumSet.noneOf(Modifier.class),
                ImmutableList.<Field>of(),
                ImmutableList.of(method),
                null,
                new JumpTarget(FAKE_FILE_ID, Position.ZERO));
    }
}
