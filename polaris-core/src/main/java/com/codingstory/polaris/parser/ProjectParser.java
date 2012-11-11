package com.codingstory.polaris.parser;


import com.codingstory.polaris.IdGenerator;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Scans source files under a base directory, and resolves type/method references.
 */
public class ProjectParser {
    public interface TypeCollector {
        void collectType(File file, List<ClassType> types);
    }

    public interface AnnotatedSourceCollector {
        void collectSource(SourceFile source);
    }

    public interface UsageCollector {
        void collectUsage(File file, List<Usage> usages);
    }

    public static class Stats {
        public int successFiles;
        public int failedFiles;
        public int types;
        public int bytes;
        public int lines;
    }

    private static class Task {
        File file;
        long fileId;
    }

    /** Resolves type references at project level. */
    private static class ProjectTypeResolver implements TypeResolver {
        private Map<FullTypeName, TypeHandle> types = Maps.newHashMap();

        public void register(TypeHandle type) {
            Preconditions.checkNotNull(type);
            types.put(type.getName(), type);
        }

        @Override
        public TypeHandle resolve(FullTypeName name) {
            Preconditions.checkNotNull(name);
            TypeHandle type = types.get(name);
            if (type == null) {
                return null;
            }
            LOG.debug(String.format("Resolved %s as %s (project reference)",
                    name,
                    type.getName()));
            return type;
        }
    }

    private static final TypeCollector NO_OP_TYPE_COLLECTOR = new TypeCollector() {
        @Override
        public void collectType(File file, List<ClassType> tokens) {
            // No-op
        }
    };

    private static final AnnotatedSourceCollector NO_OP_ANNOTATED_SOURCE_COLLECTOR = new AnnotatedSourceCollector() {
        @Override
        public void collectSource(SourceFile source) {
            // No-op
        }
    };

    private static final UsageCollector NO_OP_USAGE_COLLECTOR = new UsageCollector() {
        @Override
        public void collectUsage(File file, List<Usage> usages) {
            // No-op
        }
    };

    private static final Log LOG = LogFactory.getLog(ProjectParser.class);

    private ParserOptions parserOptions = new ParserOptions();
    private TypeCollector typeCollector = NO_OP_TYPE_COLLECTOR;
    private AnnotatedSourceCollector annotatedSourceCollector = NO_OP_ANNOTATED_SOURCE_COLLECTOR;
    private UsageCollector usageCollector = NO_OP_USAGE_COLLECTOR;
    private Stats stats;
    private List<Task> tasks = Lists.newArrayList();
    private ProjectTypeResolver projectTypeResolver;
    private TypeResolver externalTypeResolver = TypeResolver.NO_OP_RESOLVER;
    private String projectName;
    private IdGenerator idGenerator;
    private File baseDir;

    public void setTypeCollector(TypeCollector typeCollector) {
        this.typeCollector = Preconditions.checkNotNull(typeCollector);
    }

    public void setAnnotatedSourceCollector(AnnotatedSourceCollector annotatedSourceCollector) {
        this.annotatedSourceCollector = Preconditions.checkNotNull(annotatedSourceCollector);
    }

    public void setUsageCollector(UsageCollector usageCollector) {
        this.usageCollector = usageCollector;
    }

    public void setParserOptions(ParserOptions parserOptions) {
        this.parserOptions = Preconditions.checkNotNull(parserOptions);
    }

    public void setExternalTypeResolver(TypeResolver externalTypeResolver) {
        this.externalTypeResolver = Preconditions.checkNotNull(externalTypeResolver);
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public void addSourceFile(File file) {
        Preconditions.checkNotNull(file);
        Task sf = new Task();
        sf.file = file;
        sf.fileId = -1;
        tasks.add(sf);
    }

    public void setProjectBaseDirectory(File baseDir) {
        this.baseDir = baseDir;
    }

    public void run() throws IOException {
        Preconditions.checkNotNull(tasks);
        Preconditions.checkNotNull(projectName);
        Preconditions.checkNotNull(idGenerator);
        int firstPassCompleted = 0;
        prepare();
        for (Task f : tasks) {
            try {
                f.fileId = idGenerator.next();
                runFirstPass(f);
                stats.successFiles++;
                firstPassCompleted++;
                maybeLogProgress("First pass", firstPassCompleted);
            } catch (IOException e) {
                if (parserOptions.isFailFast()) {
                    throw e;
                } else {
                    stats.failedFiles++;
                }
            }
        }
        int secondPassCompleted = 0;
        for (Task f : tasks) {
            try {
                runSecondPass(f);
                stats.successFiles++;
                secondPassCompleted++;
                maybeLogProgress("Second pass", secondPassCompleted);
            } catch (IOException e) {
                if (parserOptions.isFailFast()) {
                    throw e;
                } else {
                    stats.failedFiles++;
                }
            }
        }
        stats.successFiles /= 2;
        stats.failedFiles /= 2;
    }

    private void prepare() {
        stats = new Stats();
        projectTypeResolver = new ProjectTypeResolver();
    }

    private void runFirstPass(Task task) throws IOException {
        LOG.debug("Parsing " + task.file + " (1st pass)");
        InputStream in = new FileInputStream(task.file);
        try {
            List<TypeHandle> types = FirstPassProcessor.process(in, idGenerator);
            for (TypeHandle type : types) {
                projectTypeResolver.register(type);
                LOG.debug("Found type: " + type.getName());
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void runSecondPass(Task task) throws IOException {
        LOG.debug("Parsing " + task + " (2nd pass)");
        byte[] content = FileUtils.readFileToByteArray(task.file);
        SecondPassProcessor.Result result = SecondPassProcessor.extract(
                projectName,
                task.fileId,
                new ByteArrayInputStream(content),
                new CascadeTypeResolver(ImmutableList.of(projectTypeResolver, externalTypeResolver)),
                idGenerator);
        List<ClassType> types = result.getClassTypes();
        List<Usage> usages = result.getUsages();
        LOG.debug("Found " + types.size() + " types(s) and " + usages.size() + " token(s)");
        typeCollector.collectType(task.file, types);
        usageCollector.collectUsage(task.file, usages);

        String annotatedSource = SourceAnnotator.annotate(new ByteArrayInputStream(content), usages);
        String path;
        if (baseDir != null) {
            path = StringUtils.removeStart(task.file.getAbsolutePath(), baseDir.getAbsolutePath());
        } else {
            path = task.file.getPath();
        }
        annotatedSourceCollector.collectSource(new SourceFile(
                task.fileId,
                projectName,
                path,
                new String(content),
                annotatedSource));
    }

    public Stats getStats() {
        return stats;
    }

    private void maybeLogProgress(String pass, int completed) {
        int total = tasks.size();
        boolean shouldLog = false;
        double logEveryPercent = 0.1;
        if (completed == total) {
            shouldLog = true;
        }
        if ((int)(completed / logEveryPercent / total) > (int)((completed - 1) / logEveryPercent / total)) {
            shouldLog = true;
        }
        if (shouldLog) {
            LOG.info(String.format("%s: %.2f%%", pass, 100.0 * completed / total));
        }
    }

}
