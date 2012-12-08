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
    private static final int RETRY_COUNT = 10;

    public static void crawlRepository(String user, String repoName, File outputDir) throws IOException {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(repoName);
        Preconditions.checkNotNull(outputDir);
        Repository gitHubRepo = REPOSITORY_SERVICE.getRepository(user, repoName);
        gitCloneOrPullRetry(gitHubRepo.getCloneUrl(), outputDir);
    }

    public static void crawlRepositoriesOfUser(String user, File outputDir) throws IOException {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(outputDir);
        Preconditions.checkArgument(outputDir.isDirectory());
        for (Repository repo : REPOSITORY_SERVICE.getRepositories(user)) {
            File newOutputDir = new File(outputDir, repo.getName());
            gitCloneOrPullRetry(repo.getCloneUrl(), newOutputDir);
        }
    }

    private static void gitCloneOrPullRetry(String cloneUrl, File outputDir) throws IOException {
        int currentRetryCount = 0;
        while (currentRetryCount < RETRY_COUNT) {
            try {
                if (new File(outputDir, ".git").isDirectory()) {
                    // TODO: verify url
                    gitPull(outputDir);
                } else {
                    gitClone(cloneUrl, outputDir);
                }
                return;
            } catch (IOException e) {
                LOG.warn(e);
            }
            currentRetryCount++;
        }
    }

    private static void gitPull(File outputDir) throws IOException {
        LOG.info("Updating " + outputDir);
        Git git = Git.open(outputDir);
        git.pull();
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
