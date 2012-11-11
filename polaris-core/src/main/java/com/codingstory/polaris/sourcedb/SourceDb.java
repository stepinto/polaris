package com.codingstory.polaris.sourcedb;

import com.codingstory.polaris.parser.SourceFile;

import java.io.IOException;
import java.util.List;

public interface SourceDb {
    List<String> listDirectory(String project, String path) throws IOException;
    SourceFile querySourceById(long fileId) throws IOException;
    SourceFile querySourceByPath(String project, String path) throws IOException;
}
