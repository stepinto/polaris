package com.codingstory.polaris.indexing;

import com.google.common.base.Preconditions;

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

    public interface Visitor {
        void visit(File file);
    }

    private DirectoryTranverser() {}

    /** Visits all files inside a given directory. */
    public static void traverse(File dir, Visitor visitor) {
        Preconditions.checkNotNull(dir);
        Preconditions.checkArgument(dir.isDirectory());
        Preconditions.checkNotNull(visitor);

        Queue<File> queue = new LinkedList<File>();
        queue.offer(dir);
        File f;
        while ((f = queue.poll()) != null) {
            visitor.visit(f);
            File[] subfiles = f.listFiles();
            if (subfiles != null) {
                for (File subFile : subfiles) {
                    queue.offer(subFile);
                }
            }
        }
    }
}
