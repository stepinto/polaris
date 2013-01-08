package com.codingstory.polaris.cli;

import org.apache.thrift.transport.TSocket;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandUtils {
    private CommandUtils() {}

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("([A-Za-z0-9\\.])+:([0-9])+");

    public static void die(String message) {
        System.err.println("polaris: " + message);
        System.exit(1);
    }

    public static TSocket openSocket(String address) {
        Matcher m = ADDRESS_PATTERN.matcher(address);
        if (!m.matches()) {
            throw new IllegalArgumentException("Address: " + address);
        }
        String host = m.group(1);
        int port = Integer.parseInt(m.group(2));
        return new TSocket(host, port);
    }
}
