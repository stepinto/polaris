package com.codingstory.polaris.typedb;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.SimpleIdGenerator;
import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.ClassTypeHandle;
import com.codingstory.polaris.parser.ParserProtos.Variable;
import com.codingstory.polaris.parser.ParserProtos.VariableHandle;
import com.codingstory.polaris.parser.ParserProtos.FileHandle;
import com.codingstory.polaris.parser.ParserProtos.JumpTarget;
import com.codingstory.polaris.parser.ParserProtos.Method;
import com.codingstory.polaris.parser.ParserProtos.MethodHandle;
import com.codingstory.polaris.parser.PrimitiveTypes;
import com.codingstory.polaris.parser.TypeUtils;
import com.codingstory.polaris.search.SearchProtos.Hit;
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
import java.util.List;

import static com.codingstory.polaris.TestUtils.assertEqualsIgnoreOrder;
import static com.codingstory.polaris.parser.TypeUtils.handleOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TypeDbTest {
    private static final IdGenerator ID_GENERATOR = new SimpleIdGenerator();
    private static final FileHandle FAKE_FILE = FileHandle.newBuilder()
            .setKind(FileHandle.Kind.NORMAL_FILE)
            .setId(100L)
            .setProject("project")
            .setPath("/somefile")
            .build();
    private static final FileHandle FAKE_FILE2 = FileHandle.newBuilder()
            .setKind(FileHandle.Kind.NORMAL_FILE)
            .setId(101L)
            .setProject("project")
            .setPath("/anotherfile")
            .build();
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
            types.add(createEmptyClass("MyClass" + i));
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
            types.add(createEmptyClass("MyClass"));
        }
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        for (ClassType t : types) {
            w.write(t);
        }
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        List<ClassType> result = Lists.newArrayList(r.getTypeByName("MyClass", null, 10));
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
        w.write(createClassInFile("A", FAKE_FILE));
        w.write(createClassInFile("B", FAKE_FILE));
        w.write(createClassInFile("C", FAKE_FILE2));
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        List<ClassType> types = r.getTypesInFile(FAKE_FILE.getId(), Integer.MAX_VALUE);
        assertEquals(ImmutableSet.of("A", "B"),
                ImmutableSet.copyOf(getFullTypeNames(types)));
        r.close();
    }

    private void doTestCompleteQuery(String query,
            Iterable<String> corpus, Iterable<String> expected) throws IOException {
        FileUtils.cleanDirectory(tempDir);
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        for (String type : corpus) {
            w.write(createEmptyClass(type));
        }
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        List<Hit> hits = r.query(query, Integer.MAX_VALUE);
        List<String> expectedTypes = Lists.newArrayList();
        for (String s : expected) {
            expectedTypes.add(s);
        }
        List<String> actualTypes = Lists.newArrayList();
        for (Hit hit : hits) {
            if (hit.getKind() == Hit.Kind.TYPE) {
                actualTypes.add(hit.getClassType().getHandle().getName());
            }
        }
        assertEqualsIgnoreOrder(expectedTypes, actualTypes);
        r.close();
    }

    @Test
    public void testGetFieldById() throws IOException {
        TypeDbWriter w = new TypeDbWriterImpl(tempDir);
        int n = 2;
        List<ClassType> types = Lists.newArrayList();
        for (int i = 0; i < n; i++) {
            ClassType type = createClassWithOneField("MyClass" + i, "myField");
            types.add(type);
            w.write(type);
        }
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        for (ClassType type : types) {
            Variable f = r.getFieldById(Iterables.getOnlyElement(type.getFieldsList()).getHandle().getId());
            assertNotNull(f);
            assertEquals(type.getHandle().getName() + ".myField", f.getHandle().getName());
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
            ClassType type = createClassWithOneMethod("MyClass" + i, "myMethod");
            types.add(type);
            w.write(type);
        }
        w.close();
        TypeDb r = new TypeDbImpl(tempDir);
        for (ClassType type : types) {
            Method m = r.getMethodById(Iterables.getOnlyElement(type.getMethodsList()).getHandle().getId());
            assertNotNull(m);
            assertEquals(type.getHandle().getName() + ".myMethod", m.getHandle().getName());
        }
        assertNull(r.getFieldById(Long.MAX_VALUE));
        r.close();
    }

    private static List<String> getFullTypeNames(List<ClassType> classTypes) {
        List<String> result = Lists.newArrayList();
        for (ClassType classType : classTypes) {
            result.add(classType.getHandle().getName());
        }
        return result;
    }

    private static ClassType createEmptyClass(String name) throws IOException {
        return createClassInFile(name, FAKE_FILE);
    }

    private static ClassType createClassInFile(String name, FileHandle file) throws IOException {
        ClassTypeHandle clazzHandle = ClassTypeHandle.newBuilder()
                .setId(ID_GENERATOR.next())
                .setName(name)
                .setResolved(true)
                .build();
        JumpTarget jumpTarget = JumpTarget.newBuilder()
                .setFile(file)
                .setSpan(TypeUtils.ZERO_SPAN)
                .build();
        return ClassType.newBuilder()
                .setHandle(clazzHandle)
                .setKind(ClassType.Kind.CLASS)
                .setJumpTarget(jumpTarget)
                .build();
    }

    private ClassType createClassWithOneField(String className, String fieldName) throws IOException {
        VariableHandle fieldHandle = VariableHandle.newBuilder()
                .setId(ID_GENERATOR.next())
                .setName(className + "." + fieldName)
                .build();
        JumpTarget jumpTarget = JumpTarget.newBuilder()
                .setFile(FAKE_FILE)
                .setSpan(TypeUtils.ZERO_SPAN)
                .build();
        Variable field = Variable.newBuilder()
                .setType(handleOf(PrimitiveTypes.INTEGER))
                .setHandle(fieldHandle)
                .setJumpTarget(jumpTarget)
                .build();
        return createEmptyClass(className)
                .toBuilder()
                .addFields(field)
                .build();
    }

    private ClassType createClassWithOneMethod(String className, String methodName) throws IOException {
        MethodHandle methodHandle = MethodHandle.newBuilder()
                .setId(ID_GENERATOR.next())
                .setName(className + "." + methodName)
                .build();
        JumpTarget jumpTarget = JumpTarget.newBuilder()
                .setFile(FAKE_FILE)
                .setSpan(TypeUtils.ZERO_SPAN)
                .build();
        Method method = Method.newBuilder()
                .setHandle(methodHandle)
                .setReturnType(handleOf(PrimitiveTypes.INTEGER))
                .setJumpTarget(jumpTarget)
                .build();
        return createEmptyClass(className)
                .toBuilder()
                .addMethods(method)
                .build();
    }
}
