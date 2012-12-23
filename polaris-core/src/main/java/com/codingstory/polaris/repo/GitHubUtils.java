package com.codingstory.polaris.repo;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;
import java.util.List;

public class GitHubUtils {
    private static final RepositoryService GITHUB = new RepositoryService();

    private GitHubUtils() {}

    public static Repository getRepository(String user, String repo) throws IOException {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(repo);
        return new Repository(repo, GITHUB.getRepository(user, repo).getCloneUrl());
    }

    public static List<Repository> listUserRepositories(String user) throws IOException {
        Preconditions.checkNotNull(user);
        List<Repository> repos = Lists.newArrayList();
        for (org.eclipse.egit.github.core.Repository gitHubRepo : GITHUB.getRepositories(user)) {
            repos.add(new Repository(gitHubRepo.getName(), gitHubRepo.getCloneUrl()));
        }
        return repos;
    }
}
