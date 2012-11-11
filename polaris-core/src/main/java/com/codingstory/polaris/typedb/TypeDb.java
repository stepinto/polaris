package com.codingstory.polaris.typedb;

import com.codingstory.polaris.parser.ClassType;
import com.codingstory.polaris.parser.FullTypeName;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface TypeDb extends Closeable {
    ClassType queryByTypeId(long id) throws IOException;
    List<ClassType> queryByTypeName(FullTypeName type) throws IOException;
    List<ClassType> queryFuzzy(String project, FullTypeName type, int n) throws IOException;
    List<ClassType> queryForAutoCompletion(String query, int n) throws IOException;
}
