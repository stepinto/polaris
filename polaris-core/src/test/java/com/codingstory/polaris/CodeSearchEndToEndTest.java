package com.codingstory.polaris;

import com.codingstory.polaris.indexing.IndexBuilder;
import com.codingstory.polaris.indexing.IndexPathUtils;
import com.codingstory.polaris.parser.ClassType;
import com.codingstory.polaris.parser.FullTypeName;
import com.codingstory.polaris.parser.TTypeUsage;
import com.codingstory.polaris.parser.TypeUsage;
import com.codingstory.polaris.search.CodeSearchServiceImpl;
import com.codingstory.polaris.search.TCodeSearchService;
import com.codingstory.polaris.search.TLayoutRequest;
import com.codingstory.polaris.search.TLayoutResponse;
import com.codingstory.polaris.search.TListTypeUsagesRequest;
import com.codingstory.polaris.search.TListTypeUsagesResponse;
import com.codingstory.polaris.search.TSearchRequest;
import com.codingstory.polaris.search.TSearchResponse;
import com.codingstory.polaris.search.TSourceRequest;
import com.codingstory.polaris.search.TSourceResponse;
import com.codingstory.polaris.search.TStatusCode;
import com.codingstory.polaris.typedb.TypeDb;
import com.codingstory.polaris.typedb.TypeDbImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
    public void testSource() throws IOException, TException {
        String randomJavaPath = "/src/java/main/com/java/util/Random.java";
        String randomJavaContent = "package java.util; class Random { public int nextInt() { return 0; } }";
        writeFile("jdk" + randomJavaPath, randomJavaContent);
        String fileJavaPath = "/src/java/main/com/java/io/File.java";
        String fileJavaContent = "package java.io; class File { public String getName() { return \"a\"; } }";
        writeFile("jdk" + fileJavaPath, fileJavaContent);
        buildIndex(ImmutableList.of("jdk"));

        TCodeSearchService.Iface searcher = createSearcher();
        TSourceRequest req = new TSourceRequest();
        req.setProjectName("jdk");
        req.setFileName("/src/java/main/com/java/io/File.java");
        TSourceResponse resp = searcher.source(req);
        assertEquals(TStatusCode.OK, resp.getStatus());
        assertEquals("jdk", resp.getSource().getProject());
        assertEquals(fileJavaPath, resp.getSource().getPath());
        assertEquals(fileJavaContent, resp.getSource().getSource());
    }

    @Test
    public void testLayout() throws IOException, TException {
        writeFile("project/src/com/company/A.java", "");
        writeFile("project/src/com/company/module1/B.java", "");
        buildIndex(ImmutableList.of("project"));

        TCodeSearchService.Iface searcher = createSearcher();
        TLayoutRequest req = new TLayoutRequest();
        req.setProjectName("project");
        req.setDirectoryName("/src/com/company");
        TLayoutResponse resp = searcher.layout(req);
        assertEquals(TStatusCode.OK, resp.getStatus());
        assertEquals(ImmutableSet.of("/src/com/company/A.java", "/src/com/company/module1/"),
                ImmutableSet.copyOf(resp.getChildren()));
    }

    @Test
    public void testSearchForType() throws IOException, TException {
        writeFile("project/src/com/company/A.java", "package com.company; class A {}");
        buildIndex(ImmutableList.of("project"));

        TCodeSearchService.Iface searcher = createSearcher();
        TSearchRequest req = new TSearchRequest();
        req.setQuery("com.company.A");
        TSearchResponse resp =  searcher.search(req);
        assertEquals(TStatusCode.OK, resp.getStatus());
        assertEquals(1, resp.getCount());
        assertEquals("/src/com/company/A.java", resp.getHits().get(0).getPath());
    }

    @Test
    public void testListTypeUsages() throws IOException, TException {
        writeFile("project/src/com/company/A.java", "package com.company; class A {}");
        writeFile("project/src/com/company/B.java", "package com.company; class B { A a; }");
        buildIndex(ImmutableList.of("project"));

        TypeDb typeDb = new TypeDbImpl(IndexPathUtils.getTypeDbPath(indexDir));
        ClassType type = Iterables.getOnlyElement(typeDb.getTypeByName(FullTypeName.of("com.company.A")));
        long typeId = type.getHandle().getId();
        TCodeSearchService.Iface searcher = createSearcher();
        TListTypeUsagesRequest req = new TListTypeUsagesRequest();
        req.setTypeId(typeId);
        TListTypeUsagesResponse resp = searcher.listTypeUsages(req);
        assertEquals(TStatusCode.OK, resp.getStatus());
        boolean found = false;
        for (TTypeUsage t : resp.getUsages()) {
            TypeUsage usage = TypeUsage.createFromThrift(t);
            if (usage.getKind() == TypeUsage.Kind.FIELD) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    private TCodeSearchService.Iface createSearcher() throws IOException {
        return new CodeSearchServiceImpl(indexDir);
    }

    private void writeFile(String path, String content) throws IOException {
        FileUtils.write(new File(tempDir, path), content);
    }

    private void buildIndex(List<String> projects) throws IOException {
        List<File> projectDirs = Lists.newArrayList();
        for (String project : projects) {
            projectDirs.add(new File(tempDir, project));
        }
        IndexBuilder indexBuilder = new IndexBuilder();
        indexBuilder.setIndexDirectory(indexDir);
        indexBuilder.setProjectDirectories(projectDirs);
        indexBuilder.build();
    }
}
