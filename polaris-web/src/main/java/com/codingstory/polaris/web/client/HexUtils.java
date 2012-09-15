package com.codingstory.polaris.web.client;

import com.google.common.base.Preconditions;

public class HexUtils {
    public static byte[] stringToHex(String s) {
        Preconditions.checkArgument(s.length() % 2 == 0);
        byte[] h = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            char ch1 = s.charAt(i);
            char ch2 = s.charAt(i + 1);
            h[i / 2] = (byte)(hexDigit(ch1) * 16 + hexDigit(ch2));
        }
        return h;
    }

    public static String hexToString(byte[] h) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < h.length; i++) {
            int b = h[i];
            if (b < 0) {
                b += 256;
            }
            s.append(hexChar(b / 16));
            s.append(hexChar(b % 16));
        }
        return s.toString();
    }

    private static int hexDigit(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        } else if ('A' <= ch && ch <= 'F') {
            return ch - 'A' + 10;
        } else if ('a' <= ch && ch <= 'f') {
            return ch - 'a' + 10;
        } else {
            Preconditions.checkArgument(false, "Bad hex char: " + ch);
            return 0;
        }
    }

    private static char hexChar(int n) {
        Preconditions.checkArgument(0 <= n && n < 16, "Bad digit: " + n);
        if (n < 10) {
            return (char)('0' + n);
        } else {
            return (char)('a' + n - 10);
        }
    }
}
