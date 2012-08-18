package com.codingstory.polaris.indexing;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-17
 * Time: 下午6:57
 * To change this template use File | Settings | File Templates.
 */
public class DirectoryTranverser {

    Queue<File> queue = new LinkedList<File>();

    public DirectoryTranverser(String dir) {
        queue.offer(new File(dir));
    }

    public File getNextFile() {
        if (queue.isEmpty())
            return null;
        File f = queue.poll();
        while (f != null && f.isDirectory()) {
            if (f.getName().startsWith(".")) {
                f = queue.poll();
                continue;
            }
            File[] filenames = f.listFiles();
            for (File subFile : filenames) {
                queue.offer(subFile);
            }
            f = queue.poll();
        }
        return f;
    }

    public static void main(String[] args) throws Exception {
        DirectoryTranverser tranverser = new DirectoryTranverser("d:/tddownload/lucene-3.6.1-src");
        File f;
        while ((f = tranverser.getNextFile()) != null) {
            if (!f.getName().endsWith(".java"))
                continue;
            System.out.println(f.getAbsolutePath());
        }
    }
}
