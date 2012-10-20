package com.codingstory.polaris.indexing;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-17
 * Time: 下午6:57
 * To change this template use File | Settings | File Templates.
 */
public class DirectoryTranverser {

    public static class FileAndFileIdPair {
        private File file;
        private FileId fileId;

        public FileAndFileIdPair(File file, FileId fileId) {
            this.file = file;
            this.fileId = fileId;
        }

        public File getFile() {
            return file;
        }

        public FileId getFileId() {
            return fileId;
        }
    }

    public static interface Visitor {
        void visitFile(File file, FileId directoryId, byte[] content);
        void visitDirectory(File file, List<FileAndFileIdPair> contents);
    }

    public static void traverse(File dir, Visitor visitor) {
        Preconditions.checkNotNull(dir);
        Preconditions.checkArgument(dir.isDirectory(), "Expect directory: " + dir);
        Preconditions.checkNotNull(visitor);
    }
}
