package com.codingstory.polaris.typedb;

import com.codingstory.polaris.parser.ClassType;
import com.codingstory.polaris.parser.FullTypeName;
import com.codingstory.polaris.parser.TypeHandle;
import com.google.common.base.Preconditions;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.xerial.snappy.Snappy;

import java.io.File;
import java.io.IOException;

public class TypeDbWriterImpl implements TypeDbWriter {
    private static final TSerializer SERIALIZER = new TSerializer(new TBinaryProtocol.Factory());
    private final IndexWriter writer;

    public TypeDbWriterImpl(File path) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, new TypeDbAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        this.writer = new IndexWriter(FSDirectory.open(path), config);
    }

    @Override
    public void write(ClassType type) throws IOException {
        Preconditions.checkNotNull(type);
        try {
            Document document = new Document();
            TypeHandle handle = type.getHandle();
            document.add(new Field(TypeDbIndexedField.TYPE_ID, String.valueOf(handle.getId()),
                    Field.Store.YES, Field.Index.ANALYZED));
            // TODO: index project name
            document.add(new Field(TypeDbIndexedField.PROJECT, "TODO", Field.Store.YES, Field.Index.ANALYZED));
            FullTypeName typeName = handle.getName();
            document.add(new Field(TypeDbIndexedField.FULL_TYPE, typeName.toString(),
                    Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field(TypeDbIndexedField.FULL_TYPE_CASE_INSENSITIVE, typeName.toString().toLowerCase(),
                    Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field(TypeDbIndexedField.TYPE_CASE_INSENSITIVE, typeName.getTypeName().toLowerCase(),
                    Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field(TypeDbIndexedField.TYPE_ACRONYM_CASE_INSENSITIVE,
                    getAcronym(typeName.getTypeName()).toLowerCase(), Field.Store.YES, Field.Index.ANALYZED));
            TTypeData typeData = new TTypeData();
            typeData.setClassType(type.toThrift());
            byte[] typeDataBinary = Snappy.compress(SERIALIZER.serialize(typeData));
            document.add(new Field(TypeDbIndexedField.TYPE_DATA, typeDataBinary));
            writer.addDocument(document);
        } catch (TException e) {
            throw new AssertionError(e);
        }
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
