package com.codingstory.polaris.webui;

import com.codingstory.polaris.search.TCodeSearchService;
import com.codingstory.polaris.search.TCompleteRequest;
import com.codingstory.polaris.search.TCompleteResponse;
import com.codingstory.polaris.search.TGetTypeRequest;
import com.codingstory.polaris.search.TGetTypeResponse;
import com.codingstory.polaris.search.TLayoutRequest;
import com.codingstory.polaris.search.TLayoutResponse;
import com.codingstory.polaris.search.TListTypesInFileRequest;
import com.codingstory.polaris.search.TListTypesInFileResponse;
import com.codingstory.polaris.search.TSearchRequest;
import com.codingstory.polaris.search.TSearchResponse;
import com.codingstory.polaris.search.TSourceRequest;
import com.codingstory.polaris.search.TSourceResponse;
import com.codingstory.polaris.search.TStatusCode;
import com.google.common.base.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TSocket;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AjaxServlet extends HttpServlet {

    private static final String SEARCHER_HOST = "127.0.0.1";
    private static final int SEARCHER_PORT = 5000;
    private static final Log LOG = LogFactory.getLog(AjaxServlet.class);
    private ThreadLocal<TCodeSearchService.Iface> searchers = new ThreadLocal<TCodeSearchService.Iface>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    private TCodeSearchService.Iface getSearcher() throws TException {
        TCodeSearchService.Iface searcher = searchers.get();
        if (searcher == null) {
            TSocket socket = new TSocket(SEARCHER_HOST, SEARCHER_PORT);
            socket.open();
            searcher = new TCodeSearchService.Client.Factory().getClient(new TBinaryProtocol(socket));
            searchers.set(searcher);
        }
        return searcher;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            LOG.info(req.getRequestURL());
            String path = req.getRequestURI();
            if (Objects.equal(path, "/ajax/complete")) {
                onComplete(req, resp);
            } else if (Objects.equal(path, "/ajax/search")) {
                onSearch(req, resp);
            } else if (Objects.equal(path, "/ajax/code")) {
                onReadSourceCode(req, resp);
            } else if (Objects.equal(path, "/ajax/type")) {
                onReadType(req, resp);
            } else if (Objects.equal(path, "/ajax/list-types-in-file")) {
                onListTypesInFile(req, resp);
            } else if (Objects.equal(path, "/ajax/list-files")) {
                onListFiles(req, resp);
            } else {
                LOG.warn("Unknown path: " + path);
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (TException e) {
            LOG.warn("Caught exception", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        LOG.info("done");
    }

    private void onComplete(HttpServletRequest req, HttpServletResponse resp) throws TException, IOException {
        TCompleteRequest rpcReq = new TCompleteRequest();
        rpcReq.setQuery(req.getParameter("query"));
        rpcReq.setLimit(Integer.parseInt(req.getParameter("limit")));
        TCompleteResponse rpcResp = getSearcher().complete(rpcReq);
        setHttpStatus(resp, rpcResp.getStatus());
        writeJson(resp, rpcResp);
    }

    private void onSearch(HttpServletRequest req, HttpServletResponse resp) throws TException, IOException {
        TSearchRequest rpcReq = new TSearchRequest();
        rpcReq.setQuery(req.getParameter("query"));
        rpcReq.setRankFrom(Integer.parseInt(req.getParameter("from")));
        rpcReq.setRankTo(Integer.parseInt(req.getParameter("to")));
        TSearchResponse rpcResp = getSearcher().search(rpcReq);
        setHttpStatus(resp, rpcResp.getStatus());
        writeJson(resp, rpcResp);
    }

    private void onReadSourceCode(HttpServletRequest req, HttpServletResponse resp) throws TException, IOException {
        TSourceRequest rpcReq = new TSourceRequest();
        rpcReq.setFileId(Long.parseLong(req.getParameter("fileId")));
        TSourceResponse rpcResp = getSearcher().source(rpcReq);
        setHttpStatus(resp, rpcResp.getStatus());
        writeJson(resp, rpcResp);
    }

    private void onReadType(HttpServletRequest req, HttpServletResponse resp) throws TException, IOException {
        TGetTypeRequest rpcReq = new TGetTypeRequest();
        rpcReq.setTypeId(Long.parseLong(req.getParameter("typeId")));
        TGetTypeResponse rpcResp = getSearcher().getType(rpcReq);
        setHttpStatus(resp, rpcResp.getStatus());
        writeJson(resp, rpcResp);
    }

    private void onListTypesInFile(HttpServletRequest req, HttpServletResponse resp) throws TException, IOException {
        TListTypesInFileRequest rpcReq = new TListTypesInFileRequest();
        rpcReq.setFileId(Long.parseLong(req.getParameter("fileId")));
        rpcReq.setLimit(Integer.parseInt(req.getParameter("limit")));
        TListTypesInFileResponse rpcResp = getSearcher().listTypesInFile(rpcReq);
        setHttpStatus(resp, rpcResp.getStatus());
        writeJson(resp, rpcResp);
    }

    private void onListFiles(HttpServletRequest req, HttpServletResponse resp) throws TException, IOException {
        TLayoutRequest rpcReq = new TLayoutRequest();
        rpcReq.setProjectName(req.getParameter("project"));
        rpcReq.setDirectoryName(req.getParameter("path"));
        TLayoutResponse rpcResp = getSearcher().layout(rpcReq);
        setHttpStatus(resp, rpcResp.getStatus());
        writeJson(resp, rpcResp);
    }

    private void setHttpStatus(HttpServletResponse resp, TStatusCode statusCode) {
        int httpCode;
        // TODO: handle other status codes
        if (statusCode == TStatusCode.OK) {
            httpCode = HttpServletResponse.SC_OK;
        } else {
            LOG.warn("Got status: " + statusCode.name());
            httpCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        resp.setStatus(httpCode);
    }

    private void writeJson(HttpServletResponse resp, TBase msg) throws IOException, TException {
        msg.write(new TSimpleJSONProtocol(new TIOStreamTransport(resp.getOutputStream())));
    }
}
