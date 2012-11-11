package com.codingstory.polaris.typedb;

import com.codingstory.polaris.IdUtils;
import com.codingstory.polaris.parser.ClassType;
import com.codingstory.polaris.parser.FullTypeName;
import com.google.common.base.Preconditions;
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
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.xerial.snappy.Snappy;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TypeDbImpl implements TypeDb {
    private static final Log LOG = LogFactory.getLog(TypeDbImpl.class);
    private static final TDeserializer DESERIALIZER = new TDeserializer(new TBinaryProtocol.Factory());
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
    public ClassType queryByTypeId(long typeId) throws IOException {
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
    public List<ClassType> queryByTypeName(FullTypeName type) throws IOException {
        Preconditions.checkNotNull(type);
        TermQuery query = new TermQuery(new Term(TypeDbIndexedField.FULL_TYPE, type.toString()));
        TopDocs result = searcher.search(query, Integer.MAX_VALUE);
        List<ClassType> resultClassTypes = Lists.newArrayList();
        for (ScoreDoc scoreDoc : result.scoreDocs) {
            resultClassTypes.add(retrieveDocument(scoreDoc.doc));
        }
        return resultClassTypes;
    }

    @Override
    public List<ClassType> queryFuzzy(String project, FullTypeName type, int n) throws IOException {
        return null;
    }

    @Override
    public List<ClassType> queryForAutoCompletion(String query, int n) throws IOException {
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
    public void close() throws IOException {
        reader.close();
        searcher.close();
    }

    private ClassType retrieveDocument(int docId) throws IOException {
        try {
            Document document = reader.document(docId);
            byte[] binaryData = document.getBinaryValue(TypeDbIndexedField.TYPE_DATA);
            TTypeData typeData = new TTypeData();
            DESERIALIZER.deserialize(typeData, Snappy.uncompress(binaryData));
            return ClassType.createFromThrift(typeData.getClassType());
        } catch (TException e) {
            throw new IOException(e);
        }
    }
}
