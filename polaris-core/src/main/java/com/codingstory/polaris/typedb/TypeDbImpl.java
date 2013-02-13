package com.codingstory.polaris.typedb;

import com.codingstory.polaris.IdUtils;
import com.codingstory.polaris.SnappyUtils;
import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.Field;
import com.codingstory.polaris.parser.ParserProtos.Method;
import com.codingstory.polaris.typedb.TypeDbProtos.TypeData;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TypeDbImpl implements TypeDb {
    private static final Log LOG = LogFactory.getLog(TypeDbImpl.class);
    private static final ImmutableList<String> FIELDS_FOR_AUTO_COMPLETION = ImmutableList.of(
            TypeDbIndexedField.TYPE_CASE_INSENSITIVE,
            TypeDbIndexedField.TYPE_ACRONYM_CASE_INSENSITIVE,
            TypeDbIndexedField.FULL_TYPE_CASE_INSENSITIVE);
    private final IndexReader reader;
    private final IndexSearcher searcher;

    public TypeDbImpl(File path) throws IOException {
        Preconditions.checkNotNull(path);
        reader = IndexReader.open(FSDirectory.open(path));
        searcher = new IndexSearcher(reader);
    }

    @Override
    public ClassType getTypeById(long typeId) throws IOException {
        IdUtils.checkValid(typeId);
        Query query = new TermQuery(new Term(TypeDbIndexedField.TYPE_ID, String.valueOf(typeId)));
        TopDocs result = searcher.search(query, 2);
        if (result.scoreDocs.length == 0) {
            return null;
        }
        if (result.scoreDocs.length > 1) {
            LOG.warn("Ambiguous type id: " + typeId);
        }
        int docId = result.scoreDocs[0].doc;
        return retrieveDocument(docId);
    }

    @Override
    public List<ClassType> getTypeByName(String type, String project, int n) throws IOException {
        Preconditions.checkNotNull(type);
        Preconditions.checkArgument(n >= 0);
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(new Term(TypeDbIndexedField.FULL_TYPE, type)), BooleanClause.Occur.MUST);
        if (!Strings.isNullOrEmpty(project)) {
            query.add(new TermQuery(new Term(TypeDbIndexedField.PROJECT, project)), BooleanClause.Occur.MUST);
        }
        TopDocs result = searcher.search(query, n);
        List<ClassType> resultClassTypes = Lists.newArrayList();
        for (ScoreDoc scoreDoc : result.scoreDocs) {
            resultClassTypes.add(retrieveDocument(scoreDoc.doc));
        }
        return resultClassTypes;
    }

    @Override
    public List<ClassType> queryFuzzy(String project, String type, int n) throws IOException {
        return null;
    }

    @Override
    public List<ClassType> completeQuery(String query, int n) throws IOException {
        Preconditions.checkNotNull(query);
        Preconditions.checkArgument(n >= 0);
        BooleanQuery booleanQuery = new BooleanQuery();
        for (String field : FIELDS_FOR_AUTO_COMPLETION) {
            booleanQuery.add(new PrefixQuery(
                    new Term(field, query.toLowerCase())), // case-insensitive
                    BooleanClause.Occur.SHOULD);
        }
        TopDocs hits = searcher.search(booleanQuery, n);
        List<ClassType> result = Lists.newArrayList();
        for (ScoreDoc scoreDoc: hits.scoreDocs) {
            result.add(retrieveDocument(scoreDoc.doc));
        }
        return result;
    }

    @Override
    public List<ClassType> getTypesInFile(long fileId, int n) throws IOException {
        IdUtils.checkValid(fileId);
        Query query = new TermQuery(new Term(TypeDbIndexedField.FILE_ID, String.valueOf(fileId)));
        TopDocs hits = searcher.search(query, n);
        List<ClassType> result = Lists.newArrayList();
        for (ScoreDoc scoreDoc: hits.scoreDocs) {
            result.add(retrieveDocument(scoreDoc.doc));
        }
        return result;
    }

    @Override
    public Field getFieldById(long id) throws IOException {
        IdUtils.checkValid(id);
        Query query = new TermQuery(new Term(TypeDbIndexedField.FIELD_ID, String.valueOf(id)));
        TopDocs hits = searcher.search(query, 2);
        if (hits.scoreDocs.length == 0) {
            return null;
        }
        if (hits.scoreDocs.length > 1) {
            LOG.warn("Ambiguous field id: " + id);
        }
        ClassType classType = retrieveDocument(hits.scoreDocs[0].doc);
        for (Field field : classType.getFieldsList()) {
            if (field.getHandle().getId() == id) {
                return field;
            }
        }
        LOG.error("Field " + id + " is indexed but not found in type " + classType.getHandle().getId());
        return null;
    }

    @Override
    public Method getMethodById(long id) throws IOException {
        IdUtils.checkValid(id);
        Query query = new TermQuery(new Term(TypeDbIndexedField.METHOD_ID, String.valueOf(id)));
        TopDocs hits = searcher.search(query, 2);
        if (hits.scoreDocs.length == 0) {
            return null;
        }
        if (hits.scoreDocs.length > 1) {
            LOG.warn("Ambiguous method id: " + id);
        }
        ClassType classType = retrieveDocument(hits.scoreDocs[0].doc);
        for (Method method : classType.getMethodsList()) {
            if (method.getHandle().getId() == id) {
                return method;
            }
        }
        LOG.error("Method " + id + " is indexed but not found in type " + classType.getHandle().getId());
        return null;
    }

    @Override
    public void close() throws IOException {
        reader.close();
        searcher.close();
    }

    private ClassType retrieveDocument(int docId) throws IOException {
        Document document = reader.document(docId);
        TypeData typeData = TypeData.parseFrom(
                SnappyUtils.uncompress(document.getBinaryValue(TypeDbIndexedField.TYPE_DATA)));
        return typeData.getClassType();
    }
}
