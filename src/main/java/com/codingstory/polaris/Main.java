package com.codingstory.polaris;

import com.codingstory.polaris.indexing.JavaIndexer;
import com.codingstory.polaris.parser.JavaTokenExtractor;
import com.codingstory.polaris.parser.Token;
import com.codingstory.polaris.search.SimpleWebServer;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                printHelp();
                System.exit(0);
            }
            String command = args[0];
            List<String> commandArgs = ImmutableList.copyOf(args).subList(1, args.length);
            if (command.equalsIgnoreCase("parse")) {
                runParse(commandArgs);
            } else if (command.equalsIgnoreCase("index")) {
                runIndex(commandArgs);
            } else if (command.equalsIgnoreCase("searchui")) {
                runSearchUI(commandArgs);
            } else {
                printHelp();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void runParse(List<String> commandArgs) throws IOException{
        for (String path : commandArgs) {
            File file = new File(path);
            InputStream in = null;
            try {
                System.out.println(file);
                in = new FileInputStream(file);
                List<Token> tokens = new JavaTokenExtractor()
                        .setInputStream(in)
                        .extractTokens();
                for (Token token : tokens) {
                    System.out.println(token);
                }
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }

    private static void runIndex(List<String> args) throws IOException {
        JavaIndexer indexer = new JavaIndexer(new File("index"));
        try {
            if (args.isEmpty()) {
                printHelp();
                System.exit(1);
            }
            for (String path : args) {
                indexer.indexDirectory(new File(path));
            }
        } finally {
            IOUtils.closeQuietly(indexer);
        }
    }

    private static void runSearchUI(List<String> args) throws InterruptedException, IOException {
        SimpleWebServer server = new SimpleWebServer();
        int port = 8080;
        server.setPort(port);
        System.out.println("Server: http://localhost:" + port);
        server.run();
    }

    private static void printHelp() {
        System.out.println("Usage:");
        System.out.println("  polaris parse dir/files...");
        System.out.println("  polaris index dir/files...");
        System.out.println("  polaris searchui");
        System.out.println();
    }
}
