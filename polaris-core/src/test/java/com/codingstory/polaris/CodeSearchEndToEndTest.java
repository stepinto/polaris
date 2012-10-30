package com.codingstory.polaris;

import com.codingstory.polaris.indexing.IndexBuilder;
import com.codingstory.polaris.indexing.layout.TLayoutNode;
import com.codingstory.polaris.indexing.layout.TLayoutNodeKind;
import com.codingstory.polaris.search.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
        assertEquals("jdk", resp.getProjectName());
        assertEquals(fileJavaPath, resp.getFileName());
        assertEquals(fileJavaContent, resp.getContent());
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
        List<TLayoutNode> nodes = Lists.newArrayList(resp.getEntries());
        Collections.sort(nodes, new Comparator<TLayoutNode>() {
            @Override
            public int compare(TLayoutNode left, TLayoutNode right) {
                return left.compareTo(right);
            }
        });
        assertEquals(2, nodes.size());
        assertEquals(TLayoutNodeKind.FILE, nodes.get(0).getKind());
        assertEquals("A.java", nodes.get(0).getName());
        assertEquals(TLayoutNodeKind.DIRECTORY, nodes.get(1).getKind());
        assertEquals("module1", nodes.get(1).getName());
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
