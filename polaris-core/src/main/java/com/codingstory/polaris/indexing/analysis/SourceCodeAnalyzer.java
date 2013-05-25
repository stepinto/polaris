package com.codingstory.polaris.indexing.analysis;

import com.codingstory.polaris.sourcedb.SourceDbIndexedField;
import com.google.common.collect.ImmutableSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

import java.io.Reader;
import java.util.Set;

public final class SourceCodeAnalyzer extends Analyzer {

    private static final SourceCodeAnalyzer INSTANCE = new SourceCodeAnalyzer();
    private static final Set<String> ANALYZED_FIELDS = ImmutableSet.of(
            SourceDbIndexedField.PROJECT_RAW,
            SourceDbIndexedField.PATH,
            SourceDbIndexedField.SOURCE_TEXT);

    private SourceCodeAnalyzer() {
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        if (ANALYZED_FIELDS.contains(fieldName)) {
            return new TokenStreamComponents(new StandardTokenizer(Version.LUCENE_43, reader));
        } else {
            return new TokenStreamComponents(new KeywordTokenizer(reader));
        }
    }

    public static SourceCodeAnalyzer getInstance() {
        return INSTANCE;
    }
}

