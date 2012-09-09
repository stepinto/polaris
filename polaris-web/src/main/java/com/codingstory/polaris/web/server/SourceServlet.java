package com.codingstory.polaris.web.server;

import com.codingstory.polaris.search.CodeSearchServiceImpl;
import com.codingstory.polaris.search.TCodeSearchService;
import com.codingstory.polaris.search.TSourceRequest;
import com.codingstory.polaris.search.TSourceResponse;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
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

public class SourceServlet extends HttpServlet {

    private static final Log LOG = LogFactory.getLog(SourceServlet.class);
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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            TSourceRequest sourceReq = new TSourceRequest();
            sourceReq.setFileId(Hex.decodeHex(req.getParameter("f").toCharArray()));
            TSourceResponse sourceResp = SEARCHER.source(sourceReq);
            resp.setContentType(CONTENT_TYPE_JSON);
            OutputStream out = resp.getOutputStream();
            out.write(JSON_SERIALIZER.serialize(sourceResp));
            out.flush();
        } catch (DecoderException e) {
            throw new ServletException(e);
        } catch (TException e) {
            throw new ServletException(e);
        }
    }
}
