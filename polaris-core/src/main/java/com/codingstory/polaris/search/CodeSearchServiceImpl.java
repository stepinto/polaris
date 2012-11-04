package com.codingstory.polaris.search;

import com.codingstory.polaris.EntityKind;
import com.codingstory.polaris.indexing.TToken;
import com.codingstory.polaris.indexing.TTokenList;
import com.codingstory.polaris.indexing.layout.TLayoutNodeList;
import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.xerial.snappy.Snappy;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.codingstory.polaris.indexing.FieldName.*;

public class CodeSearchServiceImpl implements TCodeSearchService.Iface, Closeable {

    private static final Log LOG = LogFactory.getLog(CodeSearchServiceImpl.class);
    private static final TDeserializer DESERIALIZER = new TDeserializer(new TBinaryProtocol.Factory());
    private final IndexReader reader;
    private final IndexSearcher searcher;
    private final SrcSearcher srcSearcher;

    public CodeSearchServiceImpl(File indexDirectory) throws IOException {
        Preconditions.checkNotNull(indexDirectory);
        Preconditions.checkArgument(indexDirectory.isDirectory());
        reader = IndexReader.open(FSDirectory.open(indexDirectory));
        searcher = new IndexSearcher(reader);
        srcSearcher = new SrcSearcher(reader);
    }

    @Override
    public TSearchResponse search(TSearchRequest req) throws TException {
        Preconditions.checkNotNull(req);
        TSearchResponse resp = new TSearchResponse();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            resp.setStatus(TStatusCode.OK);
            int from = req.isSetRankFrom() ? req.getRankFrom() : 0;
            int to = req.isSetRankTo() ? req.getRankTo() : 20;
            resp.setEntries(srcSearcher.search(req.getQuery(), from, to));
            resp.setLatency(stopWatch.getTime());
        } catch (Exception e) {
            LOG.warn("Caught exception", e);
            resp.setStatus(TStatusCode.UNKNOWN_ERROR);
        }
        return resp;
    }

    @Override
    public TSourceResponse source(TSourceRequest req) throws TException {
        Preconditions.checkNotNull(req);
        TSourceResponse resp = new TSourceResponse();
        try {
            if (!req.isSetProjectName() || !req.isSetFileName()) {
                resp.setStatus(TStatusCode.MISSING_FIELDS);
                return resp;
            }
            BooleanQuery query = new BooleanQuery();
            query.add(new TermQuery(new Term(PROJECT_NAME, req.getProjectName())), BooleanClause.Occur.MUST);
            query.add(new TermQuery(new Term(FILE_NAME, req.getFileName())), BooleanClause.Occur.MUST);
            ScoreDoc[] scoreDocs = searcher.search(query, 1).scoreDocs;
            if (scoreDocs.length > 1) {
                LOG.error("Found more than one source files matching: "
                        + req.getProjectName() + req.getFileName()); // TODO: join path
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
            resp.setContent(new String(Snappy.uncompress(doc.getBinaryValue(FILE_CONTENT))));
            resp.setAnnotations(new String(Snappy.uncompress(doc.getBinaryValue(SOURCE_ANNOTATIONS))));
            resp.setFileId(doc.getBinaryValue(FILE_ID));
            resp.setDirectoryName(doc.get(DIRECTORY_NAME));
        } catch (Exception e) {
            LOG.warn("Caught exception", e);
            resp.setStatus(TStatusCode.UNKNOWN_ERROR);
        }
        return resp;
    }

    @Override
    public TCompleteResponse complete(TCompleteRequest req) throws TException {
        Preconditions.checkNotNull(req);
        TCompleteResponse resp = new TCompleteResponse();
        try {
            resp.setStatus(TStatusCode.OK);
            resp.setEntries(srcSearcher.completeQuery(req.getQuery(), req.getLimit()));
        } catch (Exception e) {
            LOG.warn("Caught exception", e);
            resp.setStatus(TStatusCode.UNKNOWN_ERROR);
        }
        return resp;
    }

    @Override
    public TLayoutResponse layout(TLayoutRequest req) throws TException {
        Preconditions.checkNotNull(req);
        TLayoutResponse resp = new TLayoutResponse();
        try {
            if (!req.isSetProjectName() || !req.isSetDirectoryName()) {
                resp.setStatus(TStatusCode.MISSING_FIELDS);
                return resp;
            }
            BooleanQuery query = new BooleanQuery();
            query.add(new TermQuery(new Term(ENTITY_KIND, String.valueOf(EntityKind.DIRECTORY_LAYOUT.getValue()))),
                    BooleanClause.Occur.MUST);
            query.add(new TermQuery(new Term(PROJECT_NAME, req.getProjectName())), BooleanClause.Occur.MUST);
            query.add(new TermQuery(new Term(DIRECTORY_NAME, req.getDirectoryName())), BooleanClause.Occur.MUST);
            ScoreDoc[] scoreDocs = searcher.search(query, 1).scoreDocs;
            if (scoreDocs.length == 0) {
                resp.setStatus(TStatusCode.FILE_NOT_FOUND);
                return resp;
            }
            else if (scoreDocs.length != 1) {
                resp.setStatus(TStatusCode.UNKNOWN_ERROR);
                return resp;
            }
            int docId = scoreDocs[0].doc;
            Document doc = reader.document(docId);
            TLayoutNodeList nodes = new TLayoutNodeList();
            DESERIALIZER.deserialize(nodes, doc.getBinaryValue(DIRECTORY_LAYOUT));
            resp.setStatus(TStatusCode.OK);
            resp.setEntries(nodes.getNodes());
            return resp;
        } catch (Exception e) {
            LOG.error("Caught exception", e);
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
        IOUtils.closeQuietly(reader);
        IOUtils.closeQuietly(searcher);
        IOUtils.closeQuietly(srcSearcher);
    }
}
