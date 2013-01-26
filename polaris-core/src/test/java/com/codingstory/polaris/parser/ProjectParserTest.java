package com.codingstory.polaris.parser;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.SimpleIdGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProjectParserTest {
    private static class TestCollector implements ProjectParser.TypeCollector,
            ProjectParser.AnnotatedSourceCollector, ProjectParser.UsageCollector {
        private List<ClassType> types = Lists.newArrayList();
        private List<Usage> usages = Lists.newArrayList();
        private List<String> sources = Lists.newArrayList();

        @Override
        public void collectType(File file, List<ClassType> types) {
            assertNotNull(file);
            assertNotNull(types);
            this.types.addAll(types);
        }

        public List<ClassType> getTypes() {
            return types;
        }

        @Override
        public void collectSource(SourceFile sourceFile) {
            assertNotNull(sourceFile);
            sources.add(sourceFile.getAnnotatedSource());
        }

        public List<String> getSources() {
            return sources;
        }

        @Override
        public void collectUsage(File file, List<Usage> usages) {
            this.usages.addAll(usages);
        }

        public List<Usage> getUsages() {
            return usages;
        }
    }

    private static File tempDir;
    private static final String PROJECT_NAME = "TestProject";
    private static final IdGenerator ID_GENERATOR = new SimpleIdGenerator();

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
    }

    @Test
    public void testSingleFile() throws IOException {
        List<ClassType> classes = extractTypes(ImmutableList.of("package pkg; class A {}"));
        ClassType clazz = Iterables.getOnlyElement(classes);
        assertEquals(ClassType.Kind.CLASS, clazz.getKind());
        assertEquals(FullTypeName.of("pkg.A"), clazz.getName());
    }

    @Test
    public void testResolveTypes_primitive() throws IOException {
        List<ClassType> classes = extractTypes(ImmutableList.of("package pkg; class A { int a; }"));
        ClassType clazz = Iterables.getOnlyElement(classes);
        Field field = Iterables.getOnlyElement(clazz.getFields());
        TypeHandle type = field.getType();
        assertTrue(type.isResolved());
        assertEquals(PrimitiveType.INTEGER.getHandle(), type);
        assertEquals(PrimitiveType.INTEGER.getName(), type.getName());
    }

    @Test
    public void testResolveTypes_unresolved() throws IOException {
        List<ClassType> classes = extractTypes(ImmutableList.of("package pkg; class A { B b; }"));
        ClassType clazz = Iterables.getOnlyElement(classes);
        Field field = Iterables.getOnlyElement(clazz.getFields());
        assertFalse(field.getType().isResolved());
    }

    @Test
    public void testResolveTypes_importExplicitly() throws IOException {
        List<ClassType> classes = extractTypes(ImmutableList.of(
                "package pkg1; import pkg2.B; class A { B b; }",
                "package pkg2; class B {}; "));
        ClassType class0 = classes.get(0);
        ClassType class1 = classes.get(1);
        assertEquals(FullTypeName.of("pkg1.A"), class0.getName());
        assertEquals(FullTypeName.of("pkg2.B"), class1.getName());
        TypeHandle typeHandle = Iterables.getOnlyElement(class0.getFields()).getType();
        assertEquals(class1.getHandle(), typeHandle);
    }

    @Test
    public void testResolveTypes_importWildcard() throws IOException {
        List<ClassType> classes = extractTypes(ImmutableList.of(
                "package pkg1; import pkg2.*; class A { B b; }",
                "package pkg2; class B {}; "));
        ClassType class0 = classes.get(0);
        ClassType class1 = classes.get(1);
        assertEquals(FullTypeName.of("pkg1.A"), class0.getName());
        assertEquals(FullTypeName.of("pkg2.B"), class1.getName());
        TypeHandle typeHandle = Iterables.getOnlyElement(class0.getFields()).getType();
        assertEquals(class1.getHandle(), typeHandle);
    }

    @Test
    public void testResolveTypes_samePackageFirst() throws IOException {
        List<ClassType> classes = extractTypes(ImmutableList.of(
                "package pkg1; class A { B b; }",
                "package pkg1; class B {}; ",
                "package pkg2; class B {}; "));
        ClassType class0 = classes.get(0);
        ClassType class1 = classes.get(1);
        ClassType class2 = classes.get(2);
        assertEquals(FullTypeName.of("pkg1.A"), class0.getName());
        assertEquals(FullTypeName.of("pkg1.B"), class1.getName());
        assertEquals(FullTypeName.of("pkg2.B"), class2.getName());
        TypeHandle typeHandle = Iterables.getOnlyElement(class0.getFields()).getType();
        assertEquals(class1.getHandle(), typeHandle);
    }

    // TODO: testResolvedTypes_globalPackage
    // TODO: testResolvedTypes_javaLang

    @Test
    public void testSourceAnnotation_single() throws IOException {
        String s = "class A { A a; }";
        String t = Iterables.getOnlyElement(extractSources(ImmutableList.of(s)));
        assertTrue(t, t.contains("<type-usage type=\"A\""));
    }

    @Test
    public void testSourceAnnotation_multiple() throws IOException {
        String s1 = "class A { B b; }";
        String s2 = "class B { A a; }";
        List<String> a1 = extractSources(ImmutableList.of(s1, s2));
        List<String> a2 = extractSources(ImmutableList.of(s2, s1));
        assertTrue(a1.get(0), a1.get(0).contains("<type-usage type=\"B\""));
        assertTrue(a1.get(1), a1.get(1).contains("<type-usage type=\"A\""));
        assertTrue(a2.get(0), a2.get(0).contains("<type-usage type=\"A\""));
        assertTrue(a2.get(1), a2.get(1).contains("<type-usage type=\"B\""));
    }

    private static List<ClassType> extractTypes(List<String> sources) throws IOException {
        return extract(sources).getTypes();
    }

    private static List<String> extractSources(List<String> sources) throws IOException {
        return extract(sources).getSources();
    }

    private static TestCollector extract(List<String> sources) throws IOException {
        TestCollector collector = new TestCollector();
        ProjectParser parser = new ProjectParser();
        parser.setTypeCollector(collector);
        parser.setUsageCollector(collector);
        parser.setAnnotatedSourceCollector(collector);
        parser.setProjectName(PROJECT_NAME);
        parser.setIdGenerator(ID_GENERATOR);
        parser.setProjectBaseDirectory(tempDir);
        for (String source : sources) {
            parser.addSourceFile(createFile(source));
        }
        parser.run();
        return collector;
    }

    private static File createFile(String content) throws IOException {
        File file = File.createTempFile("test", ".java", tempDir);
        file.deleteOnExit();
        OutputStream out = new FileOutputStream(file);
        try {
            IOUtils.copy(new StringReader(content), out);
        } finally {
            IOUtils.closeQuietly(out);
        }
        return file;
    }
}
