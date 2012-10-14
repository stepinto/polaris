package com.codingstory.polaris;

import com.codingstory.polaris.indexing.IndexBuilder;
import com.codingstory.polaris.indexing.TToken;
import com.codingstory.polaris.indexing.TTokenKind;
import com.codingstory.polaris.search.*;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
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
        req.setFileId(DigestUtils.sha(fileJavaContent.getBytes()));
        TSourceResponse resp = searcher.source(req);
        assertEquals(TStatusCode.OK, resp.getStatus());
        assertEquals("jdk", resp.getProjectName());
        assertEquals(fileJavaPath, resp.getFileName());
        assertEquals(fileJavaContent, resp.getContent());
        assertEquals(1, Iterables.size(Iterables.filter(resp.getTokens(), new Predicate<TToken>() {
            @Override
            public boolean apply(TToken token) {
                return token.getKind() == TTokenKind.CLASS_DECLARATION;
            }
        })));
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
