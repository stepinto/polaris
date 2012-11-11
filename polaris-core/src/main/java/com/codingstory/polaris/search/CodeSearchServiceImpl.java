package com.codingstory.polaris.search;

import com.codingstory.polaris.indexing.IndexPathUtils;
import com.codingstory.polaris.parser.ClassType;
import com.codingstory.polaris.parser.SourceFile;
import com.codingstory.polaris.sourcedb.SourceDb;
import com.codingstory.polaris.sourcedb.SourceDbImpl;
import com.codingstory.polaris.typedb.TypeDb;
import com.codingstory.polaris.typedb.TypeDbImpl;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class CodeSearchServiceImpl implements TCodeSearchService.Iface, Closeable {

    private static final Log LOG = LogFactory.getLog(CodeSearchServiceImpl.class);
    private static final TDeserializer DESERIALIZER = new TDeserializer(new TBinaryProtocol.Factory());
    private final TypeDb typeDb;
    private final SourceDb sourceDb;
    private final SearchMixer mixer;

    public CodeSearchServiceImpl(File indexDirectory) throws IOException {
        Preconditions.checkNotNull(indexDirectory);
        Preconditions.checkArgument(indexDirectory.isDirectory());
        // reader = IndexReader.open(FSDirectory.open(indexDirectory));
        // searcher = new IndexSearcher(reader);
        // srcSearcher = new SrcSearcher(reader);
        typeDb = new TypeDbImpl(IndexPathUtils.getTypeDbPath(indexDirectory));
        sourceDb = new SourceDbImpl(IndexPathUtils.getSourceDbPath(indexDirectory));
        mixer = new SearchMixer(typeDb, sourceDb);
    }

    @Override
    public TSearchResponse search(TSearchRequest req) throws TException {
        Preconditions.checkNotNull(req);
        TSearchResponse resp = new TSearchResponse();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            resp.setStatus(TStatusCode.OK);
            if (!req.isSetQuery()) {
                resp.setStatus(TStatusCode.MISSING_FIELDS);
                return resp;
            }
            int from = req.isSetRankFrom() ? req.getRankFrom() : 0;
            int to = req.isSetRankTo() ? req.getRankTo() : 20;
            List<THit> hits = mixer.search(req.getQuery(), to);
            resp.setHits(hits.subList(from, Math.min(hits.size(), to)));
            resp.setCount(hits.size());
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
            SourceFile source;
            if (req.isSetFileId()) {
                source = sourceDb.querySourceById(req.getFileId());
            } else if (req.isSetProjectName() && req.isSetFileName()) {
                source = sourceDb.querySourceByPath(req.getProjectName(), req.getFileName());
            } else {
                resp.setStatus(TStatusCode.MISSING_FIELDS);
                return resp;
            }
            if (source == null) {
                resp.setStatus(TStatusCode.FILE_NOT_FOUND);
                return resp;
            }
            resp.setStatus(TStatusCode.OK);
            resp.setSource(source.toThrift());
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
            if (!req.isSetQuery()) {
                resp.setStatus(TStatusCode.MISSING_FIELDS);
                return resp;
            }
            int n = req.isSetLimit() ? req.getLimit() : 20;
            List<THit> hits = mixer.complete(req.getQuery(), n);
            resp.setStatus(TStatusCode.OK);
            for (THit hit : hits) {
                resp.addToEntries(hit.getQueryHint());
            }
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
            List<String> children = sourceDb.listDirectory(req.getProjectName(), req.getDirectoryName());
            if (children == null) {
                resp.setStatus(TStatusCode.FILE_NOT_FOUND);
                return resp;
            }
            resp.setStatus(TStatusCode.OK);
            resp.setChildren(children);
            return resp;
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(TStatusCode.UNKNOWN_ERROR);
            return resp;
        }
    }

    @Override
    public TReadClassTypeResponse readClassType(TReadClassTypeRequest req) throws TException {
        Preconditions.checkNotNull(req);
        TReadClassTypeResponse resp = new TReadClassTypeResponse();
        try {
            if (!req.isSetTypeId()) {
                resp.setStatus(TStatusCode.MISSING_FIELDS);
                return resp;
            }
            ClassType classType = typeDb.queryByTypeId(req.getTypeId());
            if (classType == null) {
                resp.setStatus(TStatusCode.FILE_NOT_FOUND);
                return resp;
            }
            resp.setStatus(TStatusCode.OK);
            resp.setClassType(classType.toThrift());
            return resp;
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(TStatusCode.UNKNOWN_ERROR);
            return resp;
        }
    }

    @Override
    public void close() throws IOException {
        // IOUtils.closeQuietly(reader);
        // IOUtils.closeQuietly(searcher);
        // IOUtils.closeQuietly(srcSearcher);
    }
}
