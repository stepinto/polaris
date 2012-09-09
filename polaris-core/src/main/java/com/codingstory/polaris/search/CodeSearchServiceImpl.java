package com.codingstory.polaris.search;

import com.codingstory.polaris.indexing.TToken;
import com.codingstory.polaris.indexing.TTokenList;
import com.google.common.base.Preconditions;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.codingstory.polaris.indexing.FieldName.*;

public class CodeSearchServiceImpl implements TCodeSearchService.Iface, Closeable {

    private static final Log LOGGER = LogFactory.getLog(CodeSearchServiceImpl.class);
    private final IndexReader reader;
    private final IndexSearcher searcher;

    public CodeSearchServiceImpl(File indexDirectory) throws IOException {
        Preconditions.checkNotNull(indexDirectory);
        Preconditions.checkArgument(indexDirectory.isDirectory());
        reader = IndexReader.open(FSDirectory.open(indexDirectory));
        searcher = new IndexSearcher(reader);
    }

    @Override
    public TSourceResponse source(TSourceRequest req) throws TException {
        Preconditions.checkNotNull(req);
        TSourceResponse resp = new TSourceResponse();
        try {
            byte[] fileId = req.getFileId();
            Query query = new TermQuery(new Term(FILE_ID, Hex.encodeHexString(fileId)));
            ScoreDoc[] scoreDocs = searcher.search(query, 1).scoreDocs;
            if (scoreDocs.length > 1) {
                LOGGER.error("Found more than one source files matching " + fileId);
                resp.setStatus(TStatusCode.UNKNOWN_ERROR);
                return resp;
            }
            if (scoreDocs.length == 0) {
                resp.setStatus(TStatusCode.FILE_NOT_FOUND);
                return resp;
            }

            int docId = scoreDocs[0].doc;
            Document doc = reader.document(docId);
            resp.setStatus(TStatusCode.OK);
            resp.setProjectName(doc.get(PROJECT_NAME));
            resp.setFileName(doc.get(FILE_NAME));
            resp.setContent(doc.getBinaryValue(FILE_CONTENT));
            resp.setTokens(deserializeTokens(doc.getBinaryValue(TOKENS)));
            return resp;
        } catch (Exception e) {
            resp.setStatus(TStatusCode.UNKNOWN_ERROR);
            return resp;
        }
    }

   private List<TToken> deserializeTokens(byte[] bytes) throws TException {
       TDeserializer deserializer = new TDeserializer(new TBinaryProtocol.Factory());
       TTokenList tokens = new TTokenList();
       deserializer.deserialize(tokens, bytes);
       return tokens.getTokens();
    }

    @Override
    public void close() throws IOException {
        reader.close();
        searcher.close();
    }
}
