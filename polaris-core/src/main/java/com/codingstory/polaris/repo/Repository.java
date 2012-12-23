package com.codingstory.polaris.repo;

import com.google.common.base.Preconditions;

import java.io.File;

/** A Git repository. */
public class Repository {
    private final String name;
    private final String url;

    public Repository(String name, String url) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(url);
        this.url = url;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public boolean isLocal() {
        return new File(url).isDirectory();
    }

    @Override
    public String toString() {
        return name + "(" + url + ")";
    }
}
