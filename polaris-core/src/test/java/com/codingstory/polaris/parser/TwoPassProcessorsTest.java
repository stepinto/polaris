package com.codingstory.polaris.parser;

import com.codingstory.polaris.IdGenerator;
import com.codingstory.polaris.SimpleIdGenerator;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.NullOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TwoPassProcessorsTest {
    private static class SimpleTypeResolver implements TypeResolver {
        private Map<FullTypeName, TypeHandle> table;

        public SimpleTypeResolver(Iterable<TypeHandle> types) {
            table = Maps.uniqueIndex(types, new Function<TypeHandle, FullTypeName>() {
                @Override
                public FullTypeName apply(TypeHandle type) {
                    return type.getName();
                }
            });
        }

        @Override
        public TypeHandle resolve(FullTypeName name) {
            Preconditions.checkNotNull(name);
            return table.get(name);
        }
    }

    private static final String TEST_PROJECT = "TestProject";
    private static final IdGenerator ID_GENERATOR = new SimpleIdGenerator();

    @Test
    public void testEmpty() throws IOException {
        SecondPassProcessor.Result result = extractFromCode("");
        assertTrue(result.getClassTypes().isEmpty());
        assertTrue(result.getUsages().isEmpty());
    }

    @Test
    public void testClass() throws IOException {
        String code = "package pkg;\npublic class MyClass { /* body */ }\n";
        SecondPassProcessor.Result result = extractFromCode(code);
        ClassType clazz = Iterables.getOnlyElement(result.getClassTypes());
        assertEquals(FullTypeName.of("pkg.MyClass"), clazz.getName());
        assertEquals(ClassType.Kind.CLASS, clazz.getKind());
        assertEquals(13, clazz.getJumpTarget().getOffset());
        TypeUsage clazzDeclaration = findUniqueTypeUsageByKind(
                result.getUsages(), TypeUsage.Kind.TYPE_DECLARATION);
        assertEquals(clazz.getHandle(), clazzDeclaration.getType());
        assertEquals(Span.of(13, 48), clazzDeclaration.getSpan());
    }

    @Test
    public void testClass_multiple() throws IOException {
        String code = "package pkg;\nclass A {}\nclass B {}\nclass C {}\n";
        SecondPassProcessor.Result result = extractFromCode(code);
        List<ClassType> classes = result.getClassTypes();
        List<String> names = Lists.newArrayList();
        for (Type clazz: classes) {
            names.add(clazz.getName().toString());
        }
        assertEquals(ImmutableList.of("pkg.A", "pkg.B", "pkg.C"), names);
    }

    @Test
    public void testClass_noPackage() throws IOException {
        String code = "class A {}";
        ClassType clazz = extractUniqueTypeFromCode(code);
        assertEquals(FullTypeName.of("A"), clazz.getName());
    }

    @Test
    public void testClass_inheritance() throws IOException {
        String code = "class A extends B implements C, D {}";
        SecondPassProcessor.Result result = extractFromCode(code);
        ClassType clazz = Iterables.getOnlyElement(result.getClassTypes());
        assertEquals(FullTypeName.of("A"), clazz.getName());
        List<FullTypeName> superTypeNames = Lists.newArrayList();
        for (TypeHandle superType : clazz.getSuperTypes()) {
            superTypeNames.add(superType.getName());
        }
        assertEquals(ImmutableList.of(FullTypeName.of("B"), FullTypeName.of("C"), FullTypeName.of("D")),
                superTypeNames);
        List<TypeUsage> usages = filterTypeUsagesByKind(result.getUsages(), TypeUsage.Kind.SUPER_CLASS);
        assertEquals(3, usages.size());
        assertEquals(Span.of(16, 17), usages.get(0).getSpan());
        assertEquals("B", usages.get(0).getType().getName().toString());
        assertEquals(Span.of(29, 30), usages.get(1).getSpan());
        assertEquals("C", usages.get(1).getType().getName().toString());
        assertEquals(Span.of(32, 33), usages.get(2).getSpan());
        assertEquals("D", usages.get(2).getType().getName().toString());
    }

    @Test
    public void testClass_javaDoc() throws IOException {
        String code = "/** doc */ class A {}";
        ClassType clazz = extractUniqueTypeFromCode(code);
        assertEquals(FullTypeName.of("A"), clazz.getName());
        assertEquals("doc", clazz.getJavaDoc());
    }

    @Test
    public void testInterface() throws IOException {
        String code = "package pkg; public interface I {};";
        ClassType clazz = extractUniqueTypeFromCode(code);
        assertEquals(ClassType.Kind.INTERFACE, clazz.getKind());
        assertEquals(FullTypeName.of("pkg.I"), clazz.getName());
    }

    @Test
    public void testInnerClass() throws IOException {
        String code = "package pkg; public class A { static public class B { public static class C {} } }";
        SecondPassProcessor.Result result = extractFromCode(code);
        List<ClassType> classes = result.getClassTypes();
        assertEquals(3, classes.size());
        assertEquals(FullTypeName.of("pkg.A"), classes.get(0).getName());
        assertEquals(FullTypeName.of("pkg.A$B"), classes.get(1).getName());
        assertEquals(FullTypeName.of("pkg.A$B$C"), classes.get(2).getName());
    }

    // TODO: testClass_public
    // TODO: testClass_private
    // TODO: testClass_packagePrivate
    // TODO: testClass_abstract
    // TODO: testClass_final
    // TODO: testNestedClass
    // TODO: testNestedClass_static

    @Test
    public void testEnum() throws IOException {
        String code = "package pkg; public enum E { /* some values */ }";
        SecondPassProcessor.Result result = extractFromCode(code);
        ClassType clazz = Iterables.getOnlyElement(result.getClassTypes());
        assertEquals(ClassType.Kind.ENUM, clazz.getKind());
        assertEquals(FullTypeName.of("pkg.E"), clazz.getName());
        TypeUsage typeDeclaration = findUniqueTypeUsageByKind(result.getUsages(), TypeUsage.Kind.TYPE_DECLARATION);
        assertEquals(clazz.getHandle(), typeDeclaration.getType());
        assertEquals(Span.of(13, 48), typeDeclaration.getSpan());
    }

    // TODO: testEnum_public
    // TODO: testEnum_private
    // TODO: testEnum_packagePrivate
    // TODO: testEnumValues

    @Test
    public void testMethod() throws IOException {
        String code = "package pkg; class A { void func() {} }";
        SecondPassProcessor.Result result = extractFromCode(code);
        ClassType clazz = Iterables.getOnlyElement(result.getClassTypes());
        Method method = Iterables.getOnlyElement(clazz.getMethods());
        assertEquals(FullMemberName.of("pkg.A#func"), method.getName());
        MethodUsage methodDeclaration = findUniqueMethodUsageByKind(
                result.getUsages(), MethodUsage.Kind.METHOD_DECLARATION);
        assertEquals(method.getHandle(), methodDeclaration.getMethod());
        assertEquals(Span.of(23, 37), methodDeclaration.getSpan());
    }

    @Test
    public void testMethod_inEnum() throws IOException {
        String code = "enum E { SOME_VALUE; void f() {} }";
        ClassType clazz = extractUniqueTypeFromCode(code);
        Method method = Iterables.getOnlyElement(clazz.getMethods());
        assertEquals(FullMemberName.of("E#f"), method.getName());
    }

    @Test
    public void testMethod_complexSignature() throws IOException {
        String code = "class A { B f(C c, D d) throws E, F { return new B(); } }";
        SecondPassProcessor.Result result = extractFromCode(code);
        ClassType clazz = Iterables.getOnlyElement(result.getClassTypes());
        Method method = Iterables.getOnlyElement(clazz.getMethods());
        assertEquals(FullMemberName.of("A#f"), method.getName());
        assertEquals(FullTypeName.of("B"), method.getReturnType().getName());
        assertEquals(2, method.getParameters().size());
        Method.Parameter parameter0 = method.getParameters().get(0);
        assertEquals(FullTypeName.of("C"), parameter0.getType().getName());
        assertEquals("c", parameter0.getName());
        Method.Parameter parameter1 = method.getParameters().get(1);
        assertEquals(FullTypeName.of("D"), parameter1.getType().getName());
        assertEquals("d", parameter1.getName());
        assertEquals(2, method.getExceptions().size());
        TypeHandle exceptionType0 = method.getExceptions().get(0);
        assertEquals(FullTypeName.of("E"), exceptionType0.getName());
        TypeHandle exceptionType1 = method.getExceptions().get(1);
        assertEquals(FullTypeName.of("F"), exceptionType1.getName());
        List<TypeUsage> usages = filterTypeUsagesByKind(result.getUsages(), TypeUsage.Kind.METHOD_SIGNATURE);
        assertEquals(5, usages.size());
        assertEquals(Span.of(10, 11), usages.get(0).getSpan());
        assertEquals(FullTypeName.of("B"), usages.get(0).getType().getName());
        assertEquals(Span.of(14, 15), usages.get(1).getSpan());
        assertEquals(FullTypeName.of("C"), usages.get(1).getType().getName());
        assertEquals(Span.of(19, 20), usages.get(2).getSpan());
        assertEquals(FullTypeName.of("D"), usages.get(2).getType().getName());
        assertEquals(Span.of(31, 32), usages.get(3).getSpan());
        assertEquals(FullTypeName.of("E"), usages.get(3).getType().getName());
        assertEquals(Span.of(34, 35), usages.get(4).getSpan());
        assertEquals(FullTypeName.of("F"), usages.get(4).getType().getName());
    }

    @Test
    public void testConstructor() throws IOException {
        String code = "class A { A() {} }";
        Method method = extractUniqueMethodFromCode(code);
        assertEquals(FullMemberName.of("A#<init>"), method.getName());
    }

    @Test
    public void testStaticInitializer() throws IOException {
        String code = "class A { static { int a; } }";
        Method method = extractUniqueMethodFromCode(code);
        assertEquals(FullMemberName.of("A#<cinit>"), method.getName());
    }

    // TODO: testMethod_public
    // TODO: testMethod_private
    // TODO: testMethod_protected
    // TODO: testMethod_packagePrivate
    // TODO: testMethod_final
    // TODO: testMethod_static

    @Test
    public void testField() throws IOException {
        String code = "package pkg; class A { int n; }";
        SecondPassProcessor.Result result = extractFromCode(code);
        ClassType clazz = Iterables.getOnlyElement(result.getClassTypes());
        Field field = Iterables.getOnlyElement(clazz.getFields());
        assertEquals(FullMemberName.of("pkg.A#n"), field.getName());
        assertEquals(PrimitiveType.INTEGER.getHandle(), field.getType());
        FieldUsage fieldDeclaration = findUniqueFieldUsageByKind(result.getUsages(), FieldUsage.Kind.FIELD_DECLARATION);
        assertEquals(field.getHandle(), fieldDeclaration.getField());
        assertEquals(Span.of(27, 28), fieldDeclaration.getSpan());
        TypeUsage usage = findUniqueTypeUsageByKind(result.getUsages(), TypeUsage.Kind.FIELD);
        assertEquals(Span.of(23, 26), usage.getSpan());
        assertEquals(PrimitiveType.INTEGER.getHandle(), usage.getType());
    }

    @Test
    public void testField_fullyQualifiedType() throws IOException {
        String code = "class A { java.util.List l; }";
        Field field = extractUniqueFieldFromCode(code);
        assertEquals(FullMemberName.of("A#l"), field.getName());
        TypeHandle type = field.getType();
        assertFalse(type.isResolved());
        assertEquals(FullTypeName.of("java.util.List"), type.getName());
    }

    @Test
    public void testField_unqualifiedType() throws IOException {
        String code = "import java.util.List; class A { List l; }";
        Field field = extractUniqueFieldFromCode(code);
        assertEquals(FullMemberName.of("A#l"), field.getName());
        TypeHandle type = field.getType();
        assertFalse(type.isResolved());
        assertEquals(FullTypeName.of("java.util.List"), type.getName());
    }

    @Test
    public void testField_initialized() throws IOException {
        String code = "class A { int m = 1; }";
        Field field = extractUniqueFieldFromCode(code);
        assertEquals(FullMemberName.of("A#m"), field.getName());
    }

    // TODO: testField_multiple
    /*
    @Test
    public void testLocalVariable() throws IOException {
        String code = "package pkg; class A { void f() { int n; } }";
        List<Token> tokens = extractFromCode(code);
        LocalVariableDeclaration var = (LocalVariableDeclaration)
                findUniqueTokenOfClass(tokens, Token.Kind.LOCAL_VARIABLE_DECLARATION);
        assertEquals(Token.Span.of(38, 39), var.getSpan());
        assertEquals(FullLocalName.of("pkg.A.f.n"), var.getName());
        assertEquals("n", var.getVariableName());
        assertEquals("int", var.getTypeReference().getUnqualifiedName());
    }

    // TODO: testLocalVariable_multiple
    // TODO: testLocalVariable_array
    */

    @Test
    public void testLineMonitorInputStream() throws IOException {
        String code = "a\nbcd\nef\ng\nhij\n";
        SecondPassProcessor.LineMonitorInputStream in = new SecondPassProcessor.LineMonitorInputStream(
                new ByteArrayInputStream(code.getBytes()));
        IOUtils.copy(in, new NullOutputStream());
        assertEquals(0, in.translateLineColumnToOffset(0, 0));
        assertEquals(2, in.translateLineColumnToOffset(1, 0));
        assertEquals(3, in.translateLineColumnToOffset(1, 1));
        assertEquals(4, in.translateLineColumnToOffset(1, 2));
        assertEquals(6, in.translateLineColumnToOffset(2, 0));
        assertEquals(7, in.translateLineColumnToOffset(2, 1));
        assertEquals(9, in.translateLineColumnToOffset(3, 0));
        assertEquals(11, in.translateLineColumnToOffset(4, 0));
        assertEquals(12, in.translateLineColumnToOffset(4, 1));
        assertEquals(13, in.translateLineColumnToOffset(4, 2));
    }

    public static SecondPassProcessor.Result extractFromCode(String code) throws IOException {
        long fakeFileId = 100;
        List<TypeHandle> types = FirstPassProcessor.process(
                new ByteArrayInputStream(code.getBytes()),
                ID_GENERATOR);
        return SecondPassProcessor.extract(
                TEST_PROJECT,
                fakeFileId,
                new ByteArrayInputStream(code.getBytes()),
                new SimpleTypeResolver(types),
                ID_GENERATOR);
    }

    public static ClassType extractUniqueTypeFromCode(String code) throws IOException {
        return Iterables.getOnlyElement(extractFromCode(code).getClassTypes());
    }

    public static Method extractUniqueMethodFromCode(String code) throws IOException {
        return Iterables.getOnlyElement(extractUniqueTypeFromCode(code).getMethods());
    }

    public static Field extractUniqueFieldFromCode(String code) throws IOException {
        return Iterables.getOnlyElement(extractUniqueTypeFromCode(code).getFields());
    }

    private List<TypeUsage> filterTypeUsages(List<Usage> usages) {
        List<TypeUsage> result = Lists.newArrayList();
        for (Usage usage : usages) {
            if (usage instanceof TypeUsage) {
                result.add((TypeUsage) usage);
            }
        }
        return result;
    }

    private List<TypeUsage> filterTypeUsagesByKind(List<Usage> usages, TypeUsage.Kind kind) {
        List<TypeUsage> result = Lists.newArrayList();
        for (TypeUsage typeUsage : filterTypeUsages(usages)) {
            if (typeUsage.getKind() == kind) {
                result.add(typeUsage);
            }
        }
        return result;
    }

    private TypeUsage findUniqueTypeUsageByKind(List<Usage> usages, final TypeUsage.Kind kind) {
        return Iterables.getOnlyElement(filterTypeUsagesByKind(usages,  kind));
    }

    private List<MethodUsage> filterMethodUsages(List<Usage> usages) {
        List<MethodUsage> result = Lists.newArrayList();
        for (Usage usage : usages) {
            if (usage instanceof MethodUsage) {
                result.add((MethodUsage) usage);
            }
        }
        return result;
    }

    private List<MethodUsage> filterMethodUsagesByKind(List<Usage> usages, MethodUsage.Kind kind) {
        List<MethodUsage> result = Lists.newArrayList();
        for (MethodUsage methodUsage : filterMethodUsages(usages)) {
            if (methodUsage.getKind() == kind) {
                result.add(methodUsage);
            }
        }
        return result;
    }

    private MethodUsage findUniqueMethodUsageByKind(List<Usage> usages, MethodUsage.Kind kind) {
        return Iterables.getOnlyElement(filterMethodUsagesByKind(usages, kind));
    }

    private List<FieldUsage> filterFieldUsages(List<Usage> usages) {
        List<FieldUsage> result = Lists.newArrayList();
        for (Usage usage : usages) {
            if (usage instanceof FieldUsage) {
                result.add((FieldUsage) usage);
            }
        }
        return result;
    }

    private List<FieldUsage> filterFieldUsagesByKind(List<Usage> usages, FieldUsage.Kind kind) {
        List<FieldUsage> result = Lists.newArrayList();
        for (FieldUsage methodUsage : filterFieldUsages(usages)) {
            if (methodUsage.getKind() == kind) {
                result.add(methodUsage);
            }
        }
        return result;
    }

    private FieldUsage findUniqueFieldUsageByKind(List<Usage> usages, FieldUsage.Kind kind) {
        return Iterables.getOnlyElement(filterFieldUsagesByKind(usages, kind));
    }
}
