package com.codingstory.polaris.typedb;

import com.codingstory.polaris.SnappyUtils;
import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.ClassTypeHandle;
import com.codingstory.polaris.parser.ParserProtos.Method;
import com.codingstory.polaris.parser.ParserProtos.Variable;
import com.codingstory.polaris.parser.TypeUtils;
import com.codingstory.polaris.typedb.TypeDbProtos.TypeData;
import com.google.common.base.Preconditions;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;

public class TypeDbWriterImpl implements TypeDbWriter {
    private final IndexWriter writer;

    public TypeDbWriterImpl(File path) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, new TypeDbAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        this.writer = new IndexWriter(FSDirectory.open(path), config);
    }

    @Override
    public void write(ClassType type) throws IOException {
        Preconditions.checkNotNull(type);
        Document document = new Document();
        ClassTypeHandle handle = type.getHandle();
        document.add(new Field(TypeDbIndexedField.TYPE_ID, String.valueOf(handle.getId()),
                Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(TypeDbIndexedField.PROJECT, type.getJumpTarget().getFile().getProject(),
                Field.Store.YES, Field.Index.ANALYZED));
        String typeName = handle.getName();
        String simpleTypeName = TypeUtils.getSimpleName(typeName);
        document.add(new Field(TypeDbIndexedField.FULL_TYPE, typeName, Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(TypeDbIndexedField.FULL_TYPE_CASE_INSENSITIVE, typeName.toLowerCase(),
                Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(TypeDbIndexedField.TYPE_CASE_INSENSITIVE, simpleTypeName.toLowerCase(),
                Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(TypeDbIndexedField.TYPE_ACRONYM_CASE_INSENSITIVE,
                getAcronym(simpleTypeName).toLowerCase(), Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(TypeDbIndexedField.FILE_ID, String.valueOf(type.getJumpTarget().getFile().getId()),
                Field.Store.YES, org.apache.lucene.document.Field.Index.ANALYZED));
        for (Variable field : type.getFieldsList()) {
            document.add(new Field(TypeDbIndexedField.FIELD_ID, String.valueOf(field.getHandle().getId()),
                    Field.Store.YES, Field.Index.ANALYZED));
        }
        for (Method method : type.getMethodsList()) {
            document.add(new Field(TypeDbIndexedField.METHOD_ID, String.valueOf(method.getHandle().getId()),
                    Field.Store.YES, Field.Index.ANALYZED));
        }
        TypeData typeData = TypeData.newBuilder()
                .setClassType(type)
                .build();
        byte[] typeDataBinary = SnappyUtils.compress(typeData.toByteArray());
        document.add(new Field(TypeDbIndexedField.TYPE_DATA, typeDataBinary));
        writer.addDocument(document);
    }

    @Override
    public void flush() throws IOException {
        writer.commit();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    private static String getAcronym(String word) {
        StringBuilder acronym = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (Character.isUpperCase(ch)) {
                acronym.append(ch);
            }
        }
        return acronym.toString();
    }
}
