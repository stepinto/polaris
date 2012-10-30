package com.codingstory.polaris.indexing;

import com.google.common.collect.ImmutableSet;

public class FieldName {
    public static final String DIRECTORY_NAME = "DirectoryName";
    public static final String DIRECTORY_LAYOUT = "DirectoryLayout";
    public static final String FIELD_NAME = "FieldName";
    public static final String FIELD_TYPE_NAME = "FieldTypeName";
    public static final String FILE_CONTENT = "FileContent";
    public static final String FILE_ID = "FileId";
    public static final String FILE_NAME = "FileName";
    public static final String JAVA_DOC = "JavaDoc";
    public static final String KIND = "Kind";
    public static final String METHOD_NAME = "Method";
    public static final String OFFSET = "Offset";
    public static final String PACKAGE_NAME = "PackageName";
    public static final String PROJECT_NAME = "ProjectName";
    public static final String SOURCE_ANNOTATIONS = "SourceAnnotations";
    public static final String TYPE_NAME = "TypeName";
    public static final String ENTITY_KIND = "EntityKind";
    // TODO: method types

    public static final ImmutableSet<String> ALL_FIELDS = ImmutableSet.of(
            DIRECTORY_NAME,
            DIRECTORY_LAYOUT,
            FIELD_NAME,
            FIELD_TYPE_NAME,
            FILE_CONTENT,
            FILE_NAME,
            FILE_ID,
            JAVA_DOC,
            KIND,
            METHOD_NAME,
            OFFSET,
            PACKAGE_NAME,
            PROJECT_NAME,
            SOURCE_ANNOTATIONS,
            TYPE_NAME);
}
