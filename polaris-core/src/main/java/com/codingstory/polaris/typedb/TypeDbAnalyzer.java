package com.codingstory.polaris.typedb;

import com.google.common.collect.ImmutableSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.TokenStream;

import java.io.Reader;
import java.util.Set;

public final class TypeDbAnalyzer extends Analyzer {
    private static final Set<String> RAW_FIELDS = ImmutableSet.of(
            TypeDbIndexedField.TYPE_ID, TypeDbIndexedField.PROJECT, TypeDbIndexedField.FULL_TYPE);

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        if (RAW_FIELDS.contains(fieldName)) {
            return new KeywordTokenizer(reader);
        } else {
            return new KeywordTokenizer(reader); // TODO: use camel-cased tokenizer
        }
    }
}
