package com.codingstory.polaris.cli.command;

import com.codingstory.polaris.cli.Command;
import com.codingstory.polaris.cli.Help;
import com.codingstory.polaris.cli.Option;
import com.codingstory.polaris.cli.Run;
import com.codingstory.polaris.repo.GitHubUtils;
import com.codingstory.polaris.repo.GitUtils;
import com.codingstory.polaris.repo.Repository;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codingstory.polaris.cli.CommandUtils.die;

@Command(name = "crawlgithub")
public class CrawlGitHub {
    private static final Log LOG = LogFactory.getLog(CrawlGitHub.class);

    @Option(name = "repobase", shortName = "r", defaultValue = "repobase")
    public static String repoBase;

    @Run
    public void run(String[] args) throws IOException, InterruptedException {
        File repoBaseDir = new File(repoBase);
        if (repoBaseDir.isDirectory()) {
            LOG.info("Reuse existing repo base directory: " + repoBase);
        } else {
            repoBaseDir.mkdir();
        }
        Pattern userPattern = Pattern.compile("([A-Za-z0-9]+)");
        Pattern userRepoPattern = Pattern.compile("([A-Za-z0-9]+)/([A-Za-z0-9]+)");
        List<Repository> repos = Lists.newArrayList();
        for (String arg : args) {
            Matcher m;
            if ((m = userPattern.matcher(arg)).matches()) {
                String user = m.group(1);
                repos.addAll(GitHubUtils.listUserRepositories(user));
            } else if ((m = userRepoPattern.matcher(arg)).matches()) {
                String user = m.group(1);
                String repo = m.group(2);
                repos.add(GitHubUtils.getRepository(user, repo));
            } else {
                die("bad repo: " + arg);
            }
        }
        LOG.info("Need to clone " + repos.size() + " repo(s)");
        for (Repository repo : repos) {
            int retry = 10;
            while (retry > 0) {
                try {
                    GitUtils.mirrorOrSync(repo, repoBaseDir);
                    break;
                } catch (IOException e) {
                    if (retry >= 0) {
                        LOG.warn("Retry on exception", e);
                        Thread.sleep(10);
                        retry--;
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    @Help
    public void help() {
        System.out.println("Usage:\n" +
                "  polaris crawlgithub [--repobase=<repobase-dir>] <github-user>.. <github-user/github-repo>..\n" +
                "\n" +
                "Options:\n" +
                "  -r, --repobase           The directory to store crawled data, default: ./repobase\n" +
                "\n");
    }
}
