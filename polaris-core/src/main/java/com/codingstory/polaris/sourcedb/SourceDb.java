package com.codingstory.polaris.sourcedb;

import com.codingstory.polaris.parser.ParserProtos.FileHandle;
import com.codingstory.polaris.parser.ParserProtos.SourceFile;
import com.codingstory.polaris.search.SearchProtos.Hit;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface SourceDb extends Closeable {

    /** @return subdirectories or files, or {@code null} if the specified path is a normal file or does not exists. */
    List<FileHandle> listDirectory(String project, String path) throws IOException;
    SourceFile querySourceById(long fileId) throws IOException;
    SourceFile querySourceByPath(String project, String path) throws IOException;
    FileHandle getFileHandle(String project, String path) throws IOException;

    /** Matches {@code query} against file names or contents. */
    List<Hit> query(String query, int n) throws IOException;
}
