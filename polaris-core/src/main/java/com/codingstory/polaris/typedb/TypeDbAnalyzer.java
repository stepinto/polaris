package com.codingstory.polaris.typedb;

import com.google.common.collect.ImmutableSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordTokenizer;

import java.io.Reader;
import java.util.Set;

public final class TypeDbAnalyzer extends Analyzer {
    private static final Set<String> RAW_FIELDS = ImmutableSet.of(
            TypeDbIndexedField.TYPE_ID, TypeDbIndexedField.PROJECT, TypeDbIndexedField.FULL_TYPE);

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        if (RAW_FIELDS.contains(fieldName)) {
            return new TokenStreamComponents(new KeywordTokenizer(reader));
        } else {
            return new TokenStreamComponents(new KeywordTokenizer(reader)); // TODO: use camel-cased tokenizer
        }
    }
}
