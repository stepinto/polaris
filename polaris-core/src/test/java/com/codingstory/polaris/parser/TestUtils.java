package com.codingstory.polaris.parser;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.List;

/**
 * Common methods for testing.
 */
public final class TestUtils {
    private TestUtils() {}

    static <T extends Token> T findUniqueTokenOfKind(List<Token> tokens, final Token.Kind kind) {
        return (T) Iterables.getOnlyElement(Iterables.filter(tokens, new Predicate<Token>() {
            @Override
            public boolean apply(Token token) {
                return token.getKind() == kind;
            }
        }));
    }

    static <T extends Token> List<T> filterTokensOfKind(List<Token> tokens, final Token.Kind kind) {
        Iterable<Token> filtered = Iterables.filter(tokens, new Predicate<Token>() {
            @Override
            public boolean apply(Token token) {
                Preconditions.checkNotNull(token);
                return token.getKind() == kind;
            }
        });
        Iterable<T> converted = Iterables.transform(filtered, new Function<Token, T>() {
            @Override
            public T apply(Token token) {
                Preconditions.checkNotNull(token);
                return (T) token;
            }
        });
        return ImmutableList.copyOf(converted);
    }
}
