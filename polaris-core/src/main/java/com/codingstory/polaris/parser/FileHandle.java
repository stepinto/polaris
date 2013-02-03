package com.codingstory.polaris.parser;

import com.codingstory.polaris.IdUtils;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public final class FileHandle {
    private long id;
    private String project;
    private String path;

    public FileHandle(long id, String project, String path) {
        Preconditions.checkNotNull(path);
        Preconditions.checkArgument(path.startsWith("/"));
        this.id = IdUtils.checkValid(id);
        this.project = Preconditions.checkNotNull(project);
        this.path = path;
    }

    public static FileHandle createFromThrift(TFileHandle t) {
        Preconditions.checkNotNull(t);
        return new FileHandle(t.getId(), t.getProject(), t.getPath());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileHandle that = (FileHandle) o;
        return this.id == that.id
                && Objects.equal(this.project, that.project)
                && Objects.equal(this.path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, project, path);
    }

    public TFileHandle toThrift() {
        TFileHandle t = new TFileHandle();
        t.setId(id);
        t.setProject(project);
        t.setPath(path);
        return t;
    }

    @Override
    public String toString() {
        return project + path + "@" + id;
    }
}
