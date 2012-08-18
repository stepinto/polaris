package com.codingstory.polaris;

import com.codingstory.polaris.parser.JavaTokenExtractor;
import com.codingstory.polaris.parser.Token;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException{
        if (args.length == 0) {
            printHelp();
        }
        String command = args[0];
        List<String> commandArgs = ImmutableList.copyOf(args).subList(1, args.length);
        if (command.equalsIgnoreCase("parse")) {
            runParse(commandArgs);
        } else {
            printHelp();
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

    private static void printHelp() {
        System.out.println("Usage:");
        System.out.println("  polaris parse files...");
        System.out.println();
    }
}
