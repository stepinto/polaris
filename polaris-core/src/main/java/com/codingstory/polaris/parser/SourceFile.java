package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;

public class SourceFile {
    private final FileHandle handle;
    private final String source;
    private final String annotatedSource;

    public SourceFile(FileHandle handle, String source, String annotatedSource) {
        this.handle = Preconditions.checkNotNull(handle);
        this.source = Preconditions.checkNotNull(source);
        this.annotatedSource = Preconditions.checkNotNull(annotatedSource);
    }

    public static SourceFile createFromThrift(TSourceFile t) {
        Preconditions.checkNotNull(t);
        return new SourceFile(
                FileHandle.createFromThrift(t.getHandle()),
                t.getSource(),
                t.getAnnotatedSource());
    }

    public FileHandle getHandle() {
        return handle;
    }

    public long getId() {
        return handle.getId();
    }

    public String getProject() {
        return handle.getProject();
    }

    public String getPath() {
        return handle.getPath();
    }

    public String getSource() {
        return source;
    }

    public String getAnnotatedSource() {
        return annotatedSource;
    }

    public TSourceFile toThrift() {
        TSourceFile t = new TSourceFile();
        t.setHandle(handle.toThrift());
        t.setSource(source);
        t.setAnnotatedSource(annotatedSource);
        return t;
    }
}
