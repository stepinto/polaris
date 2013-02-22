package com.codingstory.polaris;

import com.codingstory.polaris.indexing.IndexPathUtils;
import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.Field;
import com.codingstory.polaris.parser.ParserProtos.FileHandle;
import com.codingstory.polaris.parser.ParserProtos.Method;
import com.codingstory.polaris.parser.ParserProtos.MethodUsage;
import com.codingstory.polaris.parser.ParserProtos.TypeUsage;
import com.codingstory.polaris.parser.ParserProtos.Usage;
import com.codingstory.polaris.pipeline.IndexPipeline;
import com.codingstory.polaris.search.CodeSearchImpl;
import com.codingstory.polaris.search.SearchProtos.CodeSearch;
import com.codingstory.polaris.search.SearchProtos.GetTypeRequest;
import com.codingstory.polaris.search.SearchProtos.GetTypeResponse;
import com.codingstory.polaris.search.SearchProtos.Hit;
import com.codingstory.polaris.search.SearchProtos.LayoutRequest;
import com.codingstory.polaris.search.SearchProtos.LayoutResponse;
import com.codingstory.polaris.search.SearchProtos.ListUsagesRequest;
import com.codingstory.polaris.search.SearchProtos.ListUsagesResponse;
import com.codingstory.polaris.search.SearchProtos.SearchRequest;
import com.codingstory.polaris.search.SearchProtos.SearchResponse;
import com.codingstory.polaris.search.SearchProtos.SourceRequest;
import com.codingstory.polaris.search.SearchProtos.SourceResponse;
import com.codingstory.polaris.search.SearchProtos.StatusCode;
import com.codingstory.polaris.typedb.TypeDb;
import com.codingstory.polaris.typedb.TypeDbImpl;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.protobuf.ServiceException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.codingstory.polaris.TestUtils.assertEqualsIgnoreOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CodeSearchEndToEndTest {
    File indexDir;
    File tempDir;

    @Before
    public void setUp() {
        indexDir = Files.createTempDir();
        tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
    }

    @Test
    public void testSource() throws IOException, ServiceException {
        String randomJavaPath = "/src/java/main/com/java/util/Random.java";
        String randomJavaContent = "package java.util; class Random { public int nextInt() { return 0; } }";
        writeFile("jdk" + randomJavaPath, randomJavaContent);
        String fileJavaPath = "/src/java/main/com/java/io/File.java";
        String fileJavaContent = "package java.io; class File { public String getName() { return \"a\"; } }";
        writeFile("jdk" + fileJavaPath, fileJavaContent);
        buildIndex(ImmutableList.of("jdk"));

        CodeSearch.BlockingInterface searcher = createSearcher();
        SourceRequest req = SourceRequest.newBuilder()
                .setProjectName("jdk")
                .setFileName("/src/java/main/com/java/io/File.java")
                .build();
        SourceResponse resp = searcher.source(NoOpController.getInstance(), req);
        assertEquals(StatusCode.OK, resp.getStatus());
        assertEquals("jdk", resp.getSource().getHandle().getProject());
        assertEquals(fileJavaPath, resp.getSource().getHandle().getPath());
        assertEquals(fileJavaContent, resp.getSource().getSource());
    }

    @Test
    public void testLayout() throws IOException, ServiceException {
        writeFile("project/src/com/company/A.java", "");
        writeFile("project/src/com/company/module1/B.java", "");
        buildIndex(ImmutableList.of("project"));

        CodeSearch.BlockingInterface searcher = createSearcher();
        LayoutRequest req = LayoutRequest.newBuilder()
                .setProjectName("project")
                .setDirectoryName("/src/com/company")
                .build();
        LayoutResponse resp = searcher.layout(NoOpController.getInstance(), req);
        assertEquals(StatusCode.OK, resp.getStatus());
        assertEqualsIgnoreOrder(ImmutableList.of("/src/com/company/module1/"), resp.getDirectoriesList());
        assertEqualsIgnoreOrder(ImmutableList.of("/src/com/company/A.java"),
                Lists.transform(resp.getFilesList(), new Function<FileHandle, String>() {
                    @Override
                    public String apply(FileHandle h) {
                        return h.getPath();
                    }
                }));
    }

    @Test
    public void testSearchForType() throws IOException, ServiceException {
        writeFile("project/src/com/company/A.java", "package com.company; class A {}");
        buildIndex(ImmutableList.of("project"));

        CodeSearch.BlockingInterface searcher = createSearcher();
        SearchRequest req = SearchRequest.newBuilder()
                .setQuery("com.company.A")
                .build();
        SearchResponse resp = searcher.search(NoOpController.getInstance(), req);
        assertEquals(StatusCode.OK, resp.getStatus());
        assertEquals(1, resp.getCount());
        Hit hit = Iterables.getOnlyElement(resp.getHitsList());
        assertEquals(Hit.Kind.TYPE, hit.getKind());
        assertEquals("project", hit.getJumpTarget().getFile().getProject());
        assertEquals("/src/com/company/A.java", hit.getJumpTarget().getFile().getPath());
    }

    @Test
    public void testGetType() throws IOException, ServiceException {
        writeFile("project/src/com/company/A.java", "package com.company; class A{}");
        writeFile("project/src/com/company/B.java", "package com.company; class B{}");
        buildIndex(ImmutableList.of("project"));

        CodeSearch.BlockingInterface searcher = createSearcher();
        GetTypeRequest req = GetTypeRequest.newBuilder()
                .setTypeName("com.company.A")
                .build();
        GetTypeResponse resp = searcher.getType(NoOpController.getInstance(), req);
        assertEquals(StatusCode.OK, resp.getStatus());
        ClassType clazz = resp.getClassType();
        assertEquals("com.company.A", clazz.getHandle().getName());
    }

    @Test
    public void testListUsages_type() throws IOException, ServiceException {
        writeFile("project/src/com/company/A.java", "package com.company; class A {}");
        writeFile("project/src/com/company/B.java", "package com.company; class B { A a; }");
        buildIndex(ImmutableList.of("project"));

        TypeDb typeDb = new TypeDbImpl(IndexPathUtils.getTypeDbPath(indexDir));
        ClassType type = Iterables.getOnlyElement(typeDb.getTypeByName("com.company.A", null, 2));
        long typeId = type.getHandle().getId();
        CodeSearch.BlockingInterface searcher = createSearcher();
        ListUsagesRequest req = ListUsagesRequest.newBuilder()
                .setKind(Usage.Kind.TYPE)
                .setId(typeId)
                .build();
        ListUsagesResponse resp = searcher.listUsages(NoOpController.getInstance(), req);
        assertEquals(StatusCode.OK, resp.getStatus());
        boolean found = false;
        for (Usage usage : resp.getUsagesList()) {
            if (usage.getKind() == Usage.Kind.TYPE) {
                TypeUsage tu = usage.getType();
                if (tu.getKind() == TypeUsage.Kind.FIELD) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue(found);
    }

    @Test
    public void testListUsages_method() throws IOException, ServiceException {
        writeFile("project/src/com/company/A.java", "package com.company; class A { void f() {} }");
        writeFile("project/src/com/company/B.java", "package com.company; class B { void g() { A a; a.f(); } }");
        buildIndex(ImmutableList.of("project"));

        TypeDb typeDb = new TypeDbImpl(IndexPathUtils.getTypeDbPath(indexDir));
        ClassType classA = Iterables.getOnlyElement(typeDb.getTypeByName("com.company.A", null, 2));
        Method methodF = Iterables.getOnlyElement(classA.getMethodsList());
        ListUsagesRequest req = ListUsagesRequest.newBuilder()
                .setKind(Usage.Kind.METHOD)
                .setId(methodF.getHandle().getId())
                .build();
        ListUsagesResponse resp = createSearcher().listUsages(NoOpController.getInstance(), req);
        assertEquals(StatusCode.OK, resp.getStatus());
        boolean found = false;
        for (Usage usage : resp.getUsagesList()) {
            if (usage.getKind() == Usage.Kind.METHOD) {
                MethodUsage methodUsage = usage.getMethod();
                if (methodUsage.getKind() == MethodUsage.Kind.METHOD_CALL) {
                    found = true;
                    assertEquals(methodF.getHandle(), methodUsage.getMethod());
                }
            }
        }
        assertTrue(found);
    }

    @Test
    public void testCrossProjectReference() throws IOException {
        writeFile("project1/src/com/company/A.java", "package project1; import project2.B; class A { B b; }");
        writeFile("project2/src/com/company/B.java", "package project2; import project1.A; class B { A a; }");
        buildIndex(ImmutableList.of("project1", "project2"));

        TypeDb typeDb = new TypeDbImpl(IndexPathUtils.getTypeDbPath(indexDir));
        ClassType class1 = Iterables.getOnlyElement(typeDb.getTypeByName("project1.A", "project1", 2));
        ClassType class2 = Iterables.getOnlyElement(typeDb.getTypeByName("project2.B", "project2", 2));
        Field field1 = Iterables.getOnlyElement(class1.getFieldsList());
        Field field2 = Iterables.getOnlyElement(class2.getFieldsList());
        assertEquals(class2.getHandle(), field1.getType().getClazz());
        assertEquals(class1.getHandle(), field2.getType().getClazz());
    }

    @Test
    public void testFullTextSearch() throws IOException, ServiceException {
        writeFile("project1/src/com/company/A.java", "/* search it */");
        buildIndex(ImmutableList.of("project1"));
        CodeSearch.BlockingInterface searcher = createSearcher();
        SearchRequest req = SearchRequest.newBuilder()
                .setQuery("search it")
                .build();
        SearchResponse resp = searcher.search(NoOpController.getInstance(), req);
        assertEquals(StatusCode.OK, resp.getStatus());
        assertEquals(1, resp.getHitsCount());
        Hit hit = Iterables.getOnlyElement(resp.getHitsList());
        assertEquals(Hit.Kind.FILE, hit.getKind());
        assertEquals("/src/com/company/A.java", hit.getJumpTarget().getFile().getPath());
    }

    private CodeSearch.BlockingInterface createSearcher() throws IOException {
        return new CodeSearchImpl(indexDir);
    }

    private void writeFile(String path, String content) throws IOException {
        FileUtils.write(new File(tempDir, path), content);
    }

    private void buildIndex(List<String> projects) throws IOException {
        IndexPipeline indexPipeline = null;
        try {
            indexPipeline = new IndexPipeline();
            indexPipeline.setIndexDirectory(indexDir);
            // indexPipeline.setUseMemPipeline(true);
            for (String project : projects) {
                indexPipeline.addProjectDirectory(new File(tempDir, project));
            }
            indexPipeline.run();
        } finally {
            if (indexPipeline != null) {
                indexPipeline.cleanUp();
            }
        }
    }
}
