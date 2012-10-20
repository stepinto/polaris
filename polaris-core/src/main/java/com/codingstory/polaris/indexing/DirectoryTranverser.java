package com.codingstory.polaris.indexing;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-17
 * Time: 下午6:57
 * To change this template use File | Settings | File Templates.
 */
public class DirectoryTranverser {

    public static interface Visitor {
        public void visit(File file);
    }

    public static void traverse(File dir, Visitor visitor) {
        Preconditions.checkNotNull(dir);
        Preconditions.checkArgument(dir.isDirectory(), "Expect directory: " + dir);
        Preconditions.checkNotNull(visitor);
    }
}
