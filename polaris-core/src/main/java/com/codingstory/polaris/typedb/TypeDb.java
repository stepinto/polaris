package com.codingstory.polaris.typedb;

import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.Field;
import com.codingstory.polaris.parser.ParserProtos.Method;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface TypeDb extends Closeable {
    ClassType getTypeById(long id) throws IOException;
    List<ClassType> getTypeByName(String type, String project, int n) throws IOException;
    List<ClassType> queryFuzzy(String project, String type, int n) throws IOException;
    List<ClassType> completeQuery(String query, int n) throws IOException;
    List<ClassType> getTypesInFile(long fileId, int n) throws IOException;
    Field getFieldById(long id) throws IOException;
    Method getMethodById(long id) throws IOException;
}
