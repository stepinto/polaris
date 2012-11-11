package com.codingstory.polaris.parser;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.SimpleIdGenerator;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
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
    private static class TestCollector implements ProjectParser.TypeCollector, ProjectParser.AnnotatedSourceCollector {
        private List<ClassType> types = Lists.newArrayList();
        private String annotatedSource;

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
            this.annotatedSource = Preconditions.checkNotNull(sourceFile.getAnnotatedSource());
        }

        public String getAnnotatedSource() {
            return annotatedSource;
        }
    }

    private static final String PROJECT_NAME = "TestProject";
    private static final IdGenerator ID_GENERATOR = new SimpleIdGenerator();

    @Test
    public void testSingleFile() throws IOException {
        List<ClassType> classes = parse(ImmutableList.of("package pkg; class A {}"));
        ClassType clazz = Iterables.getOnlyElement(classes);
        assertEquals(ClassType.Kind.CLASS, clazz.getKind());
        assertEquals(FullTypeName.of("pkg.A"), clazz.getName());
    }

    @Test
    public void testResolveTypes_primitive() throws IOException {
        List<ClassType> classes = parse(ImmutableList.of("package pkg; class A { int a; }"));
        ClassType clazz = Iterables.getOnlyElement(classes);
        Field field = Iterables.getOnlyElement(clazz.getFields());
        TypeHandle type = field.getType();
        assertTrue(type.isResolved());
        assertEquals(PrimitiveType.INTEGER.getHandle(), type);
        assertEquals(PrimitiveType.INTEGER.getName(), type.getName());
    }

    @Test
    public void testResolveTypes_unresolved() throws IOException {
        List<ClassType> classes = parse(ImmutableList.of("package pkg; class A { B b; }"));
        ClassType clazz = Iterables.getOnlyElement(classes);
        Field field = Iterables.getOnlyElement(clazz.getFields());
        assertFalse(field.getType().isResolved());
    }

    @Test
    public void testResolveTypes_importExplicitly() throws IOException {
        List<ClassType> classes = parse(ImmutableList.of(
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
        List<ClassType> classes = parse(ImmutableList.of(
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
        List<ClassType> classes = parse(ImmutableList.of(
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

    private static List<ClassType> parse(List<String> sources) throws IOException {
        ProjectParser parser = new ProjectParser();
        TestCollector collector = new TestCollector();
        parser.setTypeCollector(collector);
        parser.setProjectName(PROJECT_NAME);
        parser.setIdGenerator(ID_GENERATOR);
        for (String source : sources) {
            parser.addSourceFile(createFile(source));
        }
        parser.run();
        return collector.getTypes();
    }

    private static File createFile(String content) throws IOException {
        File file = File.createTempFile("test", ".java");
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
