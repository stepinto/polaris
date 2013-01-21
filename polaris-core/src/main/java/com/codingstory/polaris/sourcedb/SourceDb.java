package com.codingstory.polaris.sourcedb;

import com.codingstory.polaris.parser.FileHandle;
import com.codingstory.polaris.parser.SourceFile;
import com.google.common.base.Objects;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface SourceDb extends Closeable {

    public static final class DirectoryContent {
        private final List<String> directories;
        private final List<FileHandle> files;

        public DirectoryContent(List<String> directories, List<FileHandle> files) {
            this.directories = directories;
            this.files = files;
        }

        public List<String> getDirectories() {
            return directories;
        }

        public List<FileHandle> getFiles() {
            return files;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DirectoryContent that = (DirectoryContent) o;
            return Objects.equal(this.directories, that.directories) && Objects.equal(this.files, that.files);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(directories, files);
        }
    }

    DirectoryContent listDirectory(String project, String path) throws IOException;
    SourceFile querySourceById(long fileId) throws IOException;
    SourceFile querySourceByPath(String project, String path) throws IOException;
}
