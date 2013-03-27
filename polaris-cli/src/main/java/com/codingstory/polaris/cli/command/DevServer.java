package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.NoOpController;
import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import com.codingstory.polaris.search.CodeSearchImpl;
import com.codingstory.polaris.search.SearchProtos.CodeSearch;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.BlockingService;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.format.JsonFormat;
import de.neuland.jade4j.Jade4J;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.lesscss.LessCompiler;
import org.lesscss.LessException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Set;

import static com.codingstory.polaris.cli.CommandUtils.checkDirectoryExists;
import static com.codingstory.polaris.cli.CommandUtils.die;

@Command(name = "devserver")
public class DevServer {

    private static final Log LOG = LogFactory.getLog(DevServer.class);

    /** Reads CSS and LESS files. */
    public static class MyServlet extends HttpServlet {
        private static final Set<String> CONTROLLER_PATHS = ImmutableSet.of("/index", "/search", "/source");
        private final File webDir;
        private final CodeSearchImpl searcher;

        public MyServlet(File indexDir, File webDir) throws IOException {
            this.webDir = Preconditions.checkNotNull(webDir);
            this.searcher = new CodeSearchImpl(indexDir);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String path = req.getRequestURI();
            LOG.info(req.getMethod() + " " + path);
            if (path.endsWith(".css")) {
                handleCss(req, resp);
            } else if (path.endsWith(".html")) {
                handleJade(req, resp);
            } else if (isControllerPath(path)) {
                handleJade(req, resp);
            } else if (path.startsWith("/partials")) {
                handleJade(req, resp);
            } else if (path.endsWith(".js")) {
                handleStatic(req, resp, new File(webDir, "public"), "application/javascript");
            } else if (path.endsWith(".png")) {
                handleStatic(req, resp, new File(webDir, "public"), "image/png");
            } else if (path.endsWith(".gif")) {
                handleStatic(req, resp, new File(webDir, "public"), "image/gif");
            } else if (path.endsWith(".xml")) {
                handleStatic(req, resp, new File(webDir, "public"), "text/xml");
            } else if (path.startsWith("/api")) {
                handleAjax(req, resp);
            } else {
                LOG.info("Unsupported file path: " + path);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            LOG.info("Status code: " + resp.getStatus());
        }

        private boolean isControllerPath(String path) {
            if (Objects.equal(path, "/")) {
                return true;
            }
            for (String controllerPath : CONTROLLER_PATHS) {
                if (path.startsWith(controllerPath)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            doGet(req, resp);
        }

        private void handleCss(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            LessCompiler compiler = new LessCompiler();
            String path = req.getRequestURI();
            resp.setContentType("text/css");
            File cssFile = new File(webDir, "public/" + path);
            File lessFile = new File(webDir, "public/" + removePathSuffix(path) + ".less");
            String content;
            if (cssFile.exists()) {
                content = FileUtils.readFileToString(cssFile);
            } else if (lessFile.exists()) {
                try {
                    content = compiler.compile(FileUtils.readFileToString(lessFile));
                } catch (LessException e) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    LOG.info("File not found: " + cssFile);
                    return;
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                LOG.info("File not found: " + cssFile);
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/css");
            resp.setContentLength(content.length());
            IOUtils.write(content, resp.getOutputStream());
        }

        private void handleJade(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String path = req.getRequestURI();
            if (isControllerPath(path)) {
                path = "index.html";
            }
            File htmlFile = new File(webDir, "views/" + path);
            File jadeFile = new File(webDir, "views/" + removePathSuffix(path) + ".jade");
            String content;
            if (htmlFile.exists()) {
                content = FileUtils.readFileToString(htmlFile);
            } else if (jadeFile.exists()) {
                content = Jade4J.render(jadeFile.getPath(), ImmutableMap.<String, Object>of());
            } else {
                LOG.info("File not found: " + htmlFile);
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/html");
            resp.setContentLength(content.length());
            IOUtils.write(content, resp.getOutputStream());
        }

        private void handleStatic(
                HttpServletRequest req, HttpServletResponse resp, File rootDir, String mimeType) throws IOException {
            File file = new File(rootDir, req.getRequestURI());
            if (!file.exists()) {
                LOG.info("File not found: " + file);
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType(mimeType);
            resp.setContentLength((int) file.length());
            FileUtils.copyFile(file, resp.getOutputStream());
        }

        private void handleAjax(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                Descriptors.ServiceDescriptor serviceDesc = CodeSearch.getDescriptor();
                String method = StringUtils.removeStart(req.getRequestURI(), "/api/");
                Descriptors.MethodDescriptor methodDesc = serviceDesc.findMethodByName(method);

                if (methodDesc == null) {
                    LOG.warn("Bad method: " + method);
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                BlockingService service = CodeSearch.newReflectiveBlockingService(searcher);
                Message.Builder inputBuilder = service.getRequestPrototype(methodDesc).newBuilderForType();
                JsonFormat.merge(new InputStreamReader(req.getInputStream()), inputBuilder);

                Message output = service.callBlockingMethod(
                        methodDesc, NoOpController.getInstance(), inputBuilder.build());

                resp.setStatus(HttpServletResponse.SC_OK);
                PrintWriter out = resp.getWriter();
                JsonFormat.print(output, out);
                out.flush();
            } catch (ServiceException e) {
                throw new AssertionError(e);
            }
        }
    }

    @Option(name = "port", shortName = "h", defaultValue = "8080")
    public String portStr;
    @Option(name = "web-root", shortName = "r", defaultValue = "polaris-web")
    public String webRoot;
    @Option(name = "index-dir", shortName = "i", defaultValue = "index")
    public String indexDir;

    @Run
    public void run(String[] args) throws Exception {
        if (args.length > 0) {
            die("Require no parameters");
        }
        File indexDir = new File(this.indexDir);
        checkDirectoryExists(indexDir);
        File webDir = new File(webRoot);
        checkDirectoryExists(webDir);
        Server webServer = new Server(Integer.parseInt(portStr));
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.addServlet(new ServletHolder(new MyServlet(indexDir, webDir)), "/*");
        webServer.setHandler(contextHandler);
        webServer.start();
        LOG.info("Listening on " + portStr);
        webServer.join();
    }

    @Help
    public void help() {
        System.out.println("Usage:\n" +
                "  polaris devserver [--port PORT] [--web-root DIR] [--index-dir DIR]\n" +
                "\n" +
                "Options:\n" +
                "  -p, --port       port to listen on, default: 8080\n" +
                "  -r, --web-root   dir for static contents, default: polaris-web\n" +
                "  -i, --index-dir  index dir, default: index\n" +
                "\n");
    }

    private static String removePathSuffix(String path) {
        int dot = path.lastIndexOf(".");
        if (dot == -1) {
            return path;
        }
        return path.substring(0, dot);
    }
}
