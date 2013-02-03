package com.codingstory.polaris;

import org.iq80.snappy.Snappy;

import java.util.Arrays;

public class SnappyUtils {
    private SnappyUtils() {}

    public static byte[] compress(byte[] in) {
        byte[] out = new byte[Snappy.maxCompressedLength(in.length)];
        int len = Snappy.compress(in, 0, in.length, out, 0);
        return Arrays.copyOf(out, len);
    }

    public static byte[] uncompress(byte[] in) {
        return uncompress(in, 0, in.length);
    }

    public static byte[] uncompress(byte[] in, int offset, int length) {
        byte[] out = new byte[Snappy.getUncompressedLength(in, 0)];
        Snappy.uncompress(in, offset, length, out, 0);
        return out;
    }
}
