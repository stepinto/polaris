package com.codingstory.polaris.search;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public class SimpleWebServer {

    public static class SearchServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Preconditions.checkNotNull(req);
            Preconditions.checkNotNull(resp);
            String query = req.getParameter("q");

            if (Strings.isNullOrEmpty(query)) {
                showSearchForm(resp);
            } else {
                // TODO: Show search form and results
            }
        }

        private void showSearchForm(HttpServletResponse resp) throws IOException {
            InputStream in = SearchServlet.class.getResourceAsStream("/searchform.html");
            OutputStream out = resp.getOutputStream();
            try {
                IOUtils.copy(in, out);
            } finally {
                IOUtils.closeQuietly(in);
            }
            out.flush();
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

    private int port = 0;

    public void setPort(int port) {
        this.port = port;
    }

    public void run() throws InterruptedException, IOException {
        Server server = new Server(port);
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.addServlet(SearchServlet.class, "/");
        contextHandler.addServlet(HelloServlet.class, "/hello");
        server.setHandler(contextHandler);
        try {
            server.start();
        } catch (Exception e) {
            throw new IOException(e); // Just wrap it
        }
        server.join();
    }

}
