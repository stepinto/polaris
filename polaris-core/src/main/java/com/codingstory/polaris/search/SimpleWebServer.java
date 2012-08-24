package com.codingstory.polaris.search;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.Map;

public class SimpleWebServer {

    public static class SearchServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Preconditions.checkNotNull(req);
            Preconditions.checkNotNull(resp);
            String query = req.getParameter("q");
            boolean debug = Objects.equal(req.getParameter("debug"), "on");

            if (Strings.isNullOrEmpty(query)) {
                showSearchForm(resp);
            } else {
                StopWatch watch = new StopWatch();
                watch.start();
                List<Result> results = search(query);
                watch.stop();
                showSearchResults(query, debug, results, resp, watch.getTime());
            }
        }

        private List<Result> search(String query) throws IOException, ServletException {
            try {
                SrcSearcher searcher = new SrcSearcher("index");
                List<Result> results = searcher.search(query, 100);
                return results;
            } catch (ParseException e) {
                throw new ServletException(e);
            } catch (InvalidTokenOffsetsException e) {
                throw new ServletException(e);
            }
        }

        private void showSearchForm(HttpServletResponse resp) throws IOException {
            InputStream in = SearchServlet.class.getResourceAsStream("/SearchForm.html");
            OutputStream out = resp.getOutputStream();
            try {
                IOUtils.copy(in, out);
            } finally {
                IOUtils.closeQuietly(in);
            }
            out.flush();
        }

        private void showSearchResults(String query, boolean debug, List<Result> results,
                                       HttpServletResponse resp, long ms)
                throws ServletException, IOException {
            InputStream in = SearchServlet.class.getResourceAsStream("/SearchResult.ftl");
            try {
                Configuration conf = new Configuration();
                conf.setClassForTemplateLoading(SearchServlet.class, "/");
                Template template = conf.getTemplate("SearchResult.ftl");
                Map<String, Object> root = Maps.newHashMap();
                root.put("results", results);
                root.put("query", query);
                root.put("debug", debug);
                root.put("debug_checked_str", debug ? "checked" : "");
                root.put("seconds_str", String.format("%.2f", ms / 1000.0));
                Writer out = resp.getWriter();
                template.process(root, out);
                out.flush();
            } catch (TemplateException e) {
                throw new ServletException(e);
            } finally {
                IOUtils.closeQuietly(in);
            }
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            doGet(req, resp);
        }
    }

    public static class HelloServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Preconditions.checkNotNull(req);
            Preconditions.checkNotNull(resp);
            PrintWriter out = resp.getWriter();
            out.println("<html><body><h1>Hello!</h1></body></html>");
            out.flush();
        }
    }

    public static class SourceServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // TODO: Use our searcher interface
            Preconditions.checkNotNull(req);
            Preconditions.checkNotNull(resp);
            String filename = req.getParameter("filename");
            IndexReader reader = IndexReader.open(FSDirectory.open(new File("index")));
            PrintWriter out = resp.getWriter();
            try {
                Query query = new TermQuery(new Term("filename", filename));
                int docid = new IndexSearcher(reader).search(query, 1).scoreDocs[0].doc;
                String content = reader.document(docid).get("content");
                out.println("<html><body><pre>");
                out.println(content);
                out.println("</pre></body></html>");
                out.flush();
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }
    }

    public static class StaticServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Preconditions.checkNotNull(req);
            Preconditions.checkNotNull(resp);
            String path = req.getRequestURI();
            path = StringUtils.removeStart(path, "/static");
            InputStream in = StaticServlet.class.getResourceAsStream(path);
            OutputStream out = resp.getOutputStream();
            try {
                IOUtils.copy(in, out);
                out.flush();
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }

    private int port = 0;

    public void setPort(int port) {
        this.port = port;
    }

    public void run() throws InterruptedException, IOException {
        Server server = new Server(port);
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.addServlet(SearchServlet.class, "/");
        contextHandler.addServlet(HelloServlet.class, "/hello");
        contextHandler.addServlet(SourceServlet.class, "/source");
        contextHandler.addServlet(StaticServlet.class, "/static/main.css");
        server.setHandler(contextHandler);
        try {
            server.start();
        } catch (Exception e) {
            throw new IOException(e); // Just wrap it
        }
        server.join();
    }

}
