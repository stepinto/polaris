package com.codingstory.polaris.typedb;

import com.codingstory.polaris.parser.ClassType;
import com.codingstory.polaris.parser.Field;
import com.codingstory.polaris.parser.FullTypeName;
import com.codingstory.polaris.parser.Method;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface TypeDb extends Closeable {
    ClassType getTypeById(long id) throws IOException;
    List<ClassType> getTypeByName(FullTypeName type, String project, int n) throws IOException;
    List<ClassType> queryFuzzy(String project, FullTypeName type, int n) throws IOException;
    List<ClassType> completeQuery(String query, int n) throws IOException;
    List<ClassType> getTypesInFile(long fileId, int n) throws IOException;
    Field getFieldById(long id) throws IOException;
    Method getMethodById(long id) throws IOException;
}
