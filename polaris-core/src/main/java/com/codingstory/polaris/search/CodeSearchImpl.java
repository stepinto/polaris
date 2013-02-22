package com.codingstory.polaris.search;

import com.codingstory.polaris.indexing.IndexPathUtils;
import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.Field;
import com.codingstory.polaris.parser.ParserProtos.Method;
import com.codingstory.polaris.parser.ParserProtos.SourceFile;
import com.codingstory.polaris.search.SearchProtos.CodeSearch;
import com.codingstory.polaris.search.SearchProtos.CompleteRequest;
import com.codingstory.polaris.search.SearchProtos.CompleteResponse;
import com.codingstory.polaris.search.SearchProtos.GetFieldRequest;
import com.codingstory.polaris.search.SearchProtos.GetFieldResponse;
import com.codingstory.polaris.search.SearchProtos.GetMethodRequest;
import com.codingstory.polaris.search.SearchProtos.GetMethodResponse;
import com.codingstory.polaris.search.SearchProtos.GetTypeRequest;
import com.codingstory.polaris.search.SearchProtos.GetTypeResponse;
import com.codingstory.polaris.search.SearchProtos.Hit;
import com.codingstory.polaris.search.SearchProtos.LayoutRequest;
import com.codingstory.polaris.search.SearchProtos.LayoutResponse;
import com.codingstory.polaris.search.SearchProtos.ListTypesInFileRequest;
import com.codingstory.polaris.search.SearchProtos.ListTypesInFileResponse;
import com.codingstory.polaris.search.SearchProtos.ListUsagesRequest;
import com.codingstory.polaris.search.SearchProtos.ListUsagesResponse;
import com.codingstory.polaris.search.SearchProtos.SearchRequest;
import com.codingstory.polaris.search.SearchProtos.SearchResponse;
import com.codingstory.polaris.search.SearchProtos.SourceRequest;
import com.codingstory.polaris.search.SearchProtos.SourceResponse;
import com.codingstory.polaris.search.SearchProtos.StatusCode;
import com.codingstory.polaris.sourcedb.SourceDb;
import com.codingstory.polaris.sourcedb.SourceDbImpl;
import com.codingstory.polaris.typedb.TypeDb;
import com.codingstory.polaris.typedb.TypeDbImpl;
import com.codingstory.polaris.usagedb.UsageDb;
import com.codingstory.polaris.usagedb.UsageDbImpl;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.protobuf.RpcController;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class CodeSearchImpl implements CodeSearch.BlockingInterface, Closeable {

    private static final Log LOG = LogFactory.getLog(CodeSearchImpl.class);
    private final TypeDb typeDb;
    private final SourceDb sourceDb;
    private final UsageDb usageDb;
    private final SearchMixer mixer;

    public CodeSearchImpl(File indexDirectory) throws IOException {
        Preconditions.checkNotNull(indexDirectory);
        Preconditions.checkArgument(indexDirectory.isDirectory());
        typeDb = new TypeDbImpl(IndexPathUtils.getTypeDbPath(indexDirectory));
        sourceDb = new SourceDbImpl(IndexPathUtils.getSourceDbPath(indexDirectory));
        usageDb = new UsageDbImpl(IndexPathUtils.getUsageDbPath(indexDirectory));
        mixer = new SearchMixer(typeDb, sourceDb);
    }

    @Override
    public SearchResponse search(RpcController controller, SearchRequest req) {
        SearchResponse.Builder resp = SearchResponse.newBuilder();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            resp.setStatus(StatusCode.OK);
            if (!req.hasQuery()) {
                resp.setStatus(StatusCode.MISSING_FIELDS);
                return resp.build();
            }
            int from = req.hasRankFrom() ? req.getRankFrom() : 0;
            int to = req.hasRankTo() ? req.getRankTo() : 20;
            List<Hit> hits = mixer.search(req.getQuery(), to, true);
            resp.addAllHits(hits.subList(from, Math.min(hits.size(), to)));
            resp.setCount(hits.size());
            resp.setLatency(stopWatch.getTime());
        } catch (Exception e) {
            LOG.warn("Caught exception", e);
            resp.setStatus(StatusCode.UNKNOWN_ERROR);
        }
        return resp.build();
    }

    @Override
    public SourceResponse source(RpcController controller, SourceRequest req) {
        SourceResponse.Builder resp = SourceResponse.newBuilder();
        try {
            SourceFile source;
            if (req.hasFileId()) {
                source = sourceDb.querySourceById(req.getFileId());
            } else if (req.hasProjectName() && req.hasFileName()) {
                source = sourceDb.querySourceByPath(req.getProjectName(), req.getFileName());
            } else {
                resp.setStatus(StatusCode.MISSING_FIELDS);
                return resp.build();
            }
            if (source == null) {
                resp.setStatus(StatusCode.FILE_NOT_FOUND);
                return resp.build();
            }
            resp.setStatus(StatusCode.OK);
            resp.setSource(source);
        } catch (Exception e) {
            LOG.warn("Caught exception", e);
            resp.setStatus(StatusCode.UNKNOWN_ERROR);
        }
        return resp.build();
    }

    @Override
    public CompleteResponse complete(RpcController controller, CompleteRequest req) {
        CompleteResponse.Builder resp = CompleteResponse.newBuilder();
        try {
            if (!req.hasQuery()) {
                resp.setStatus(StatusCode.MISSING_FIELDS);
                return resp.build();
            }
            int n = req.hasLimit() ? req.getLimit() : 20;
            List<Hit> hits = mixer.search(req.getQuery(), n, false);
            resp.setStatus(StatusCode.OK);
            resp.addAllHits(hits);
        } catch (Exception e) {
            LOG.warn("Caught exception", e);
            resp.setStatus(StatusCode.UNKNOWN_ERROR);
        }
        return resp.build();
    }

    @Override
    public LayoutResponse layout(RpcController controller, LayoutRequest req) {
        LayoutResponse.Builder resp = LayoutResponse.newBuilder();
        try {
            if (!req.hasProjectName() || !req.hasDirectoryName()) {
                resp.setStatus(StatusCode.MISSING_FIELDS);
                return resp.build();
            }
            SourceDb.DirectoryContent content = sourceDb.listDirectory(req.getProjectName(), req.getDirectoryName());
            if (content == null) {
                resp.setStatus(StatusCode.FILE_NOT_FOUND);
                return resp.build();
            }
            resp.setStatus(StatusCode.OK);
            resp.addAllDirectories(content.getDirectories());
            resp.addAllFiles(content.getFiles());
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(StatusCode.UNKNOWN_ERROR);
        }
        return resp.build();
    }

    @Override
    public GetTypeResponse getType(RpcController controller, GetTypeRequest req) {
        GetTypeResponse.Builder resp = GetTypeResponse.newBuilder();
        try {
            List<ClassType> classes;
            if (req.hasTypeId()) {
                ClassType clazz = typeDb.getTypeById(req.getTypeId());
                classes = (clazz == null ? ImmutableList.<ClassType>of() : ImmutableList.of(clazz));
            } else if (req.hasTypeName()) {
                classes = typeDb.getTypeByName(
                        req.getTypeName(),
                        req.hasProject() ? req.getProject() : null,
                        2);
            } else {
                resp.setStatus(StatusCode.MISSING_FIELDS);
                return resp.build();
            }

            if (classes.isEmpty()) {
                resp.setStatus(StatusCode.FILE_NOT_FOUND);
            } else if (classes.size() > 1) {
                resp.setStatus(StatusCode.NOT_UNIQUE);
            } else {
                resp.setStatus(StatusCode.OK);
                resp.setClassType(Iterables.getOnlyElement(classes));
            }
            return resp.build();
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(StatusCode.UNKNOWN_ERROR);
            return resp.build();
        }
    }

    @Override
    public ListTypesInFileResponse listTypesInFile(RpcController controller, ListTypesInFileRequest req) {
        ListTypesInFileResponse.Builder resp = ListTypesInFileResponse.newBuilder();
        try {
            if (!req.hasFileId()) {
                resp.setStatus(StatusCode.MISSING_FIELDS);
                return resp.build();
            }
            int limit = req.hasLimit() ? req.getLimit() : 20;
            List<ClassType> classTypes = typeDb.getTypesInFile(req.getFileId(), limit);
            resp.setStatus(StatusCode.OK);
            resp.addAllClassTypes(classTypes);
            return resp.build();
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(StatusCode.UNKNOWN_ERROR);
            return resp.build();
        }
    }

    @Override
    public ListUsagesResponse listUsages(RpcController controller, ListUsagesRequest req) {
        ListUsagesResponse.Builder resp = ListUsagesResponse.newBuilder();
        try {
            if (!req.hasKind() || !req.hasId()) {
                resp.setStatus(StatusCode.MISSING_FIELDS);
                return resp.build();
            }
            resp.setStatus(StatusCode.OK);
            resp.addAllUsages(usageDb.query(req.getKind(), req.getId()));
            return resp.build();
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(StatusCode.UNKNOWN_ERROR);
            return resp.build();
        }
    }

    @Override
    public GetFieldResponse getField(RpcController controller, GetFieldRequest req) {
        GetFieldResponse.Builder resp = GetFieldResponse.newBuilder();
        try {
            if (!req.hasFieldId()) {
                resp.setStatus(StatusCode.MISSING_FIELDS);
                return resp.build();
            }
            Field field = typeDb.getFieldById(req.getFieldId());
            if (field == null) {
                resp.setStatus(StatusCode.FILE_NOT_FOUND);
                return resp.build();
            }
            resp.setStatus(StatusCode.OK);
            resp.setField(field);
            return resp.build();
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(StatusCode.UNKNOWN_ERROR);
            return resp.build();
        }
    }

    @Override
    public GetMethodResponse getMethod(RpcController controller, GetMethodRequest req) {
        GetMethodResponse.Builder resp = GetMethodResponse.newBuilder();
        try {
            if (!req.hasMethodId()) {
                resp.setStatus(StatusCode.MISSING_FIELDS);
                return resp.build();
            }
            Method method = typeDb.getMethodById(req.getMethodId());
            if (method == null) {
                resp.setStatus(StatusCode.FILE_NOT_FOUND);
                return resp.build();
            }
            resp.setStatus(StatusCode.OK);
            resp.setMethod(method);
            return resp.build();
        } catch (Exception e) {
            LOG.error("Caught exception", e);
            resp.setStatus(StatusCode.UNKNOWN_ERROR);
            return resp.build();
        }
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(typeDb);
        IOUtils.closeQuietly(sourceDb);
        IOUtils.closeQuietly(usageDb);
    }
}
