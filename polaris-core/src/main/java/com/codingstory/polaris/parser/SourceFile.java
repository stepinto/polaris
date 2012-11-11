package com.codingstory.polaris.parser;

import com.codingstory.polaris.IdUtils;
import com.google.common.base.Preconditions;

public class SourceFile {
    private final long id;
    private final String project;
    private final String path;
    private final String source;
    private final String annotatedSource;

    public SourceFile(long id, String project, String path, String source, String annotatedSource) {
        Preconditions.checkNotNull(path);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        this.id = IdUtils.checkValid(id);
        this.project = Preconditions.checkNotNull(project);
        this.path = path;
        this.source = Preconditions.checkNotNull(source);
        this.annotatedSource = Preconditions.checkNotNull(annotatedSource);
    }

    public static SourceFile createFromThrift(TSourceFile t) {
        Preconditions.checkNotNull(t);
        return new SourceFile(
                t.getId(),
                t.getProject(),
                t.getPath(),
                t.getSource(),
                t.getAnnotatedSource());
    }

    public long getId() {
        return id;
    }

    public String getProject() {
        return project;
    }

    public String getPath() {
        return path;
    }

    public String getSource() {
        return source;
    }

    public String getAnnotatedSource() {
        return annotatedSource;
    }

    public TSourceFile toThrift() {
        TSourceFile t = new TSourceFile();
        t.setId(id);
        t.setProject(project);
        t.setPath(path);
        t.setSource(source);
        t.setAnnotatedSource(annotatedSource);
        return t;
    }
}
