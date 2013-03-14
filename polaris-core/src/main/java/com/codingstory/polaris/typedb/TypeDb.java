package com.codingstory.polaris.typedb;

import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.Variable;
import com.codingstory.polaris.parser.ParserProtos.Method;
import com.codingstory.polaris.search.SearchProtos.Hit;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface TypeDb extends Closeable {
    ClassType getTypeById(long id) throws IOException;
    List<ClassType> getTypeByName(String type, String project, int n) throws IOException;
    List<ClassType> getTypesInFile(long fileId, int n) throws IOException;
    Variable getFieldById(long id) throws IOException;
    Method getMethodById(long id) throws IOException;

    /** Searches for types in {@code TypeDb}. It does not set {@code summary}. */
    List<Hit> query(String query, int n) throws IOException;
}
