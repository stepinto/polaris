package com.codingstory.polaris.web.client;

import com.google.common.base.Preconditions;

import java.util.List;

/**
 * A simple lexer for highlighting source files.
 */
public class SimpleLexer {
    public interface Rule {
        int execute(String code, int offset);
    }

    private final List<Rule> rules;

    public SimpleLexer(List<Rule> rules) {
        this.rules = Preconditions.checkNotNull(rules);
    }

    public void scan(String code) {
        Preconditions.checkNotNull(code);
        int offset = 0;
        while (offset < code.length()) {
            boolean matched = false;
            for (Rule rule : rules) {
                int n = rule.execute(code, offset);
                assert n >= 0;
                assert offset + n <= code.length();
                if (n > 0) {
                    offset += n;
                    matched = true;
                    break;
                }
            }
            assert matched;
        }
    }
}
