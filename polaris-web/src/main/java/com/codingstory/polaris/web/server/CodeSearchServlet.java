package com.codingstory.polaris.web.server;

import com.codingstory.polaris.search.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TSimpleJSONProtocol;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class CodeSearchServlet extends HttpServlet {

    private static final Log LOG = LogFactory.getLog(CodeSearchServlet.class);
    private static final TCodeSearchService.Iface SEARCHER;
    private static final TSerializer JSON_SERIALIZER = new TSerializer(new TSimpleJSONProtocol.Factory());
    private static final String CONTENT_TYPE_JSON = "application/json";

    static {
        try {
            SEARCHER = new CodeSearchServiceImpl(new File("index"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String method = req.getRequestURI();
        if (method.contains("/")) {
            method = method.substring(method.lastIndexOf('/') + 1);
        }
        if (method.equals("search")) {
            runSearch(req, resp);
        } else if (method.equals("source")) {
            runSource(req, resp);
        } else if (method.equals("complete")) {
            runComplete(req, resp);
        } else {
            LOG.warn("Bad URI: " + req.getRequestURI());
            resp.setStatus(400);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        doGet(req, resp);
    }

    private void runSearch(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        try {
            TSearchRequest searchReq = new TSearchRequest();
            searchReq.setQuery(req.getParameter("q"));
            TSearchResponse searchResp = SEARCHER.search(searchReq);
            resp.setContentType(CONTENT_TYPE_JSON);
            OutputStream out = resp.getOutputStream();
            out.write(JSON_SERIALIZER.serialize(searchResp));
        } catch (TException e) {
            throw new ServletException(e);
        }
    }

    private void runSource(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        try {
            TSourceRequest sourceReq = new TSourceRequest();
            sourceReq.setFileId(req.getParameter("f"));
            TSourceResponse sourceResp = SEARCHER.source(sourceReq);
            resp.setContentType(CONTENT_TYPE_JSON);
            OutputStream out = resp.getOutputStream();
            out.write(JSON_SERIALIZER.serialize(sourceResp));
        } catch (TException e) {
            throw new ServletException(e);
        }
    }

    private void runComplete(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        try {
            TCompleteRequest completeReq = new TCompleteRequest();
            completeReq.setQuery(req.getParameter("q"));
            completeReq.setLimit(Integer.parseInt(req.getParameter("n")));
            TCompleteResponse completeResp = SEARCHER.complete(completeReq);
            resp.setContentType(CONTENT_TYPE_JSON);
            OutputStream out = resp.getOutputStream();
            out.write(JSON_SERIALIZER.serialize(completeResp));
        } catch (TException e) {
            throw new ServletException(e);
        }
    }
}
