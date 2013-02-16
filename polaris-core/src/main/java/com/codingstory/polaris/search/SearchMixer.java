package com.codingstory.polaris.search;

import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.SourceFile;
import com.codingstory.polaris.parser.ParserProtos.Span;
import com.codingstory.polaris.search.SearchProtos.Hit;
import com.codingstory.polaris.sourcedb.SourceDb;
import com.codingstory.polaris.typedb.TypeDb;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Mixes and re-ranks searchByType results from {@link com.codingstory.polaris.typedb.TypeDb}
 * and {@link com.codingstory.polaris.sourcedb.SourceDb}.
 */
public class SearchMixer {
    private static final Log LOG = LogFactory.getLog(SearchMixer.class);
    private final TypeDb typeDb;
    private final SourceDb sourceDb;

    public SearchMixer(TypeDb typeDb, SourceDb sourceDb) {
        this.typeDb = Preconditions.checkNotNull(typeDb);
        this.sourceDb = Preconditions.checkNotNull(sourceDb);
    }

    public List<Hit> search(String query, int n, boolean search) throws IOException {
        Preconditions.checkNotNull(query);
        Preconditions.checkArgument(n >= 0);
        if (search) {
            LOG.info("Query: " + query);
        } else {
            LOG.info("Complete query: " + query);
        }
        // TODO: Need to understand query
        List<Hit> hits = Lists.newArrayList();
        hits.addAll(fillSummary(typeDb.query(query, n)));
        hits.addAll(sourceDb.query(query, n));
        sortHits(hits);
        if (hits.size() > n) {
            return hits.subList(0, n);
        } else {
            return hits;
        }
    }

    private void sortHits(List<Hit> hits) {
        Collections.sort(hits, new Comparator<Hit>() {
            @Override
            public int compare(Hit left, Hit right) {
                return Doubles.compare(left.getScore(), right.getScore());
            }
        });
    }

    private List<Hit> fillSummary(List<Hit> hits) throws IOException {
        List<Hit> results = Lists.newArrayList();
        for (Hit hit : hits) {
            ClassType clazz = hit.getClassType();
            long fileId = clazz.getJumpTarget().getFile().getId();
            SourceFile source = sourceDb.querySourceById(fileId);
            if (source == null) {
                throw new AssertionError("File #" + fileId + " does not exist in SourceDb");
            }
            results.add(hit.toBuilder()
                    .setSummary(getSummary(source.getSource(), clazz.getJumpTarget().getSpan()))
                    .build());
        }
        return results;
    }

    public static String getSummary(String content, Span span) {
        String[] lines = content.split("\n");
        int from = Math.max(span.getFrom().getLine() - 2, 0);
        int to = Math.min(span.getFrom().getLine() + 3, lines.length);
        StringBuilder result = new StringBuilder();
        for (int i = from; i < to; i++) {
            result.append(lines[i]);
            result.append("\n");
        }
        return result.toString();
    }
}
