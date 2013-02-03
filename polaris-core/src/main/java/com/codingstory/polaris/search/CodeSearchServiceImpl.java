package com.codingstory.polaris.search;

import com.codingstory.polaris.indexing.IndexPathUtils;
import com.codingstory.polaris.parser.ClassType;
import com.codingstory.polaris.parser.Field;
import com.codingstory.polaris.parser.FileHandle;
import com.codingstory.polaris.parser.FullTypeName;
import com.codingstory.polaris.parser.Method;
import com.codingstory.polaris.parser.SourceFile;
import com.codingstory.polaris.parser.TypeUsage;
import com.codingstory.polaris.sourcedb.SourceDb;
import com.codingstory.polaris.sourcedb.SourceDbImpl;
import com.codingstory.polaris.typedb.TypeDb;
import com.codingstory.polaris.typedb.TypeDbImpl;
import com.codingstory.polaris.usagedb.UsageDb;
import com.codingstory.polaris.usagedb.UsageDbImpl;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
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
    private final UsageDb usageDb;
    private final SearchMixer mixer;

    public CodeSearchServiceImpl(File indexDirectory) throws IOException {
        Preconditions.checkNotNull(indexDirectory);
        Preconditions.checkArgument(indexDirectory.isDirectory());
        typeDb = new TypeDbImpl(IndexPathUtils.getTypeDbPath(indexDirectory));
        sourceDb = new SourceDbImpl(IndexPathUtils.getSourceDbPath(indexDirectory));
        usageDb = new UsageDbImpl(IndexPathUtils.getUsageDbPath(indexDirectory));
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
            resp.setHits(hits);
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
            SourceDb.DirectoryContent content = sourceDb.listDirectory(req.getProjectName(), req.getDirectoryName());
            if (content == null) {
                resp.setStatus(TStatusCode.FILE_NOT_FOUND);
                return resp;
            }
            resp.setStatus(TStatusCode.OK);
            resp.setDirectories(content.getDirectories());
            for (FileHandle file : content.getFiles()) {
                resp.addToFiles(file.toThrift());
            }
            return resp;
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(TStatusCode.UNKNOWN_ERROR);
            return resp;
        }
    }

    @Override
    public TGetTypeResponse getType(TGetTypeRequest req) throws TException {
        Preconditions.checkNotNull(req);
        TGetTypeResponse resp = new TGetTypeResponse();
        try {
            List<ClassType> classes;
            if (req.isSetTypeId()) {
                ClassType clazz = typeDb.getTypeById(req.getTypeId());
                classes = (clazz == null ? ImmutableList.<ClassType>of() : ImmutableList.of(clazz));
            } else if (req.isSetTypeName()) {
                classes = typeDb.getTypeByName(
                        FullTypeName.of(req.getTypeName()),
                        req.isSetProject() ? req.getProject() : null,
                        2);
            } else {
                resp.setStatus(TStatusCode.MISSING_FIELDS);
                return resp;
            }

            if (classes.isEmpty()) {
                resp.setStatus(TStatusCode.FILE_NOT_FOUND);
            } else if (classes.size() > 1) {
                resp.setStatus(TStatusCode.NOT_UNIQUE);
            } else {
                resp.setStatus(TStatusCode.OK);
                resp.setClassType(Iterables.getOnlyElement(classes).toThrift());
            }
            return resp;
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(TStatusCode.UNKNOWN_ERROR);
            return resp;
        }
    }

    @Override
    public TListTypesInFileResponse listTypesInFile(TListTypesInFileRequest req) throws TException {
        Preconditions.checkNotNull(req);
        TListTypesInFileResponse resp = new TListTypesInFileResponse();
        try {
            if (!req.isSetFileId()) {
                resp.setStatus(TStatusCode.MISSING_FIELDS);
                return resp;
            }
            int limit = req.isSetLimit() ? req.getLimit() : 20;
            List<ClassType> classTypes = typeDb.getTypesInFile(req.getFileId(), limit);
            resp.setStatus(TStatusCode.OK);
            for (ClassType classType : classTypes) {
                resp.addToClassTypes(classType.toThrift());
            }
            return resp;
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(TStatusCode.UNKNOWN_ERROR);
            return resp;
        }
    }

    @Override
    public TListTypeUsagesResponse listTypeUsages(TListTypeUsagesRequest req) throws TException {
        Preconditions.checkNotNull(req);
        TListTypeUsagesResponse resp = new TListTypeUsagesResponse();
        try {
            if (!req.isSetTypeId()) {
                resp.setStatus(TStatusCode.MISSING_FIELDS);
                return resp;
            }
            resp.setStatus(TStatusCode.OK);
            List<TypeUsage> usages = usageDb.query(req.getTypeId());
            for (TypeUsage usage : usages) {
                resp.addToUsages(usage.toThrift());
            }
            return resp;
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(TStatusCode.UNKNOWN_ERROR);
            return resp;
        }
    }

    @Override
    public TGetFieldResponse getField(TGetFieldRequest req) throws TException {
        Preconditions.checkNotNull(req);
        TGetFieldResponse resp = new TGetFieldResponse();
        try {
            if (!req.isSetFieldId()) {
                resp.setStatus(TStatusCode.MISSING_FIELDS);
                return resp;
            }
            Field field = typeDb.getFieldById(req.getFieldId());
            if (field == null) {
                resp.setStatus(TStatusCode.FILE_NOT_FOUND);
                return resp;
            }
            resp.setStatus(TStatusCode.OK);
            resp.setField(field.toThrift());
            return resp;
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(TStatusCode.UNKNOWN_ERROR);
            return resp;
        }
    }

    @Override
    public TGetMethodResponse getMethod(TGetMethodRequest req) throws TException {
        Preconditions.checkNotNull(req);
        TGetMethodResponse resp = new TGetMethodResponse();
        try {
            if (!req.isSetMethodId()) {
                resp.setStatus(TStatusCode.MISSING_FIELDS);
                return resp;
            }
            Method method = typeDb.getMethodById(req.getMethodId());
            if (method == null) {
                resp.setStatus(TStatusCode.FILE_NOT_FOUND);
                return resp;
            }
            resp.setStatus(TStatusCode.OK);
            resp.setMethod(method.toThrift());
            return resp;
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(TStatusCode.UNKNOWN_ERROR);
            return resp;
        }
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(typeDb);
        IOUtils.closeQuietly(sourceDb);
        IOUtils.closeQuietly(usageDb);
    }
}
