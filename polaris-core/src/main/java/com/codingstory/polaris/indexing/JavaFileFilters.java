package com.codingstory.polaris.indexing;

import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

public class JavaFileFilters {

    public static final IOFileFilter JAVA_SOURCE_FILETER = new AndFileFilter(
            new SuffixFileFilter(".java"),
            FileFileFilter.FILE);

}
