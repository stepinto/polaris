package com.codingstory.polaris.repo;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

/** Fetches code from GitHub. */
public class GitHubCrawler {

    private static final Log LOG = LogFactory.getLog(GitHubCrawler.class);
    private static final RepositoryService REPOSITORY_SERVICE = new RepositoryService();

    public static void crawlRepository(String user, String repoName, File outputDir) throws IOException {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(repoName);
        Preconditions.checkNotNull(outputDir);
        Repository gitHubRepo = REPOSITORY_SERVICE.getRepository(user, repoName);
        gitClone(gitHubRepo.getCloneUrl(), outputDir);
    }

    public static void crawlRepositoriesOfUser(String user, File outputDir) throws IOException {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(outputDir);
        Preconditions.checkArgument(outputDir.isDirectory());
        for (Repository repo : REPOSITORY_SERVICE.getRepositories(user)) {
            File newOutputDir = new File(outputDir, repo.getName());
            gitClone(repo.getCloneUrl(), newOutputDir);
        }
    }

    private static void gitClone(String cloneUrl, File outputDir) throws IOException {
        LOG.info("Cloning " + cloneUrl);
        try {
            if (outputDir.isDirectory()) {
                LOG.info("Removing existing directory " + outputDir);
                FileUtils.deleteDirectory(outputDir);
            }
            Git.cloneRepository()
                    .setURI(cloneUrl)
                    .setDirectory(outputDir)
                    .call();
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }
}
