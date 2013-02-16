package com.codingstory.polaris.indexing.analysis;

import com.codingstory.polaris.sourcedb.SourceDbIndexedField;
import com.google.common.collect.ImmutableSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

import java.io.Reader;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: yangshuguo
 * Date: 13-2-2
 * Time: 上午2:23
 * To change this template use File | Settings | File Templates.
 */
public final class SourceCodeAnalyzer extends Analyzer {

    private static final SourceCodeAnalyzer INSTANCE = new SourceCodeAnalyzer();
    private static final Set<String> ANALYZED_FIELDS = ImmutableSet.of(
            SourceDbIndexedField.PROJECT_RAW,
            SourceDbIndexedField.PATH,
            SourceDbIndexedField.SOURCE_TEXT);

    private SourceCodeAnalyzer() {}

    public static SourceCodeAnalyzer getInstance() {
        return INSTANCE;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      if (ANALYZED_FIELDS.contains(fieldName)) {
        return new StandardTokenizer(Version.LUCENE_36, reader);
      } else {
        return new KeywordTokenizer(reader);
      }
    }
}
