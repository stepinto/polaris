package com.codingstory.polaris.search;

import com.codingstory.polaris.parser.ParserProtos.ClassType;
import com.codingstory.polaris.parser.ParserProtos.JumpTarget;
import com.codingstory.polaris.parser.ParserProtos.SourceFile;
import com.codingstory.polaris.parser.ParserProtos.Span;
import com.codingstory.polaris.search.SearchProtos.Hit;
import com.codingstory.polaris.sourcedb.SourceDb;
import com.codingstory.polaris.typedb.TypeDb;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
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

    public List<Hit> search(String query, int n) throws IOException {
        Preconditions.checkNotNull(query);
        Preconditions.checkArgument(n >= 0);
        LOG.info("Query: " + query);
        // TODO: Need to understand query
        List<Hit> result = Lists.newArrayList();
        List<ClassType> classTypes = typeDb.completeQuery(query, n); // TODO: Use dedicated approach
        for (ClassType classType : classTypes) {
            result.add(classTypeToHit(classType));
        }
        return result;
    }

    public List<Hit> searchBySource(String query, int n) throws IOException {
        // TODO: Need to merge the result to search().
        Preconditions.checkNotNull(query);
        Preconditions.checkArgument(n >= 0);
        LOG.info("Query in typeDb: " + query);
        List<Hit> results = Lists.newArrayList();
        sourceDb.querySourcesByTerm(query);

        return results;
    }

    public List<Hit> complete(String query, int n) throws IOException {
        Preconditions.checkNotNull(query);
        Preconditions.checkArgument(n >= 0);
        LOG.info("Complete query: " + query);
        List<Hit> result = Lists.newArrayList();
        List<ClassType> classTypes = typeDb.completeQuery(query, n);
        for (ClassType classType : classTypes) {
            result.add(classTypeToHit(classType));
        }
        return result;
    }

    private Hit classTypeToHit(ClassType clazz) throws IOException {
        JumpTarget jumpTarget = clazz.getJumpTarget();
        SourceFile source = sourceDb.querySourceById(jumpTarget.getFile().getId());
        Hit hit = Hit.newBuilder()
                .setProject(source.getHandle().getProject())
                .setPath(source.getHandle().getPath())
                .setJumpTarget(jumpTarget)
                .setSummary(getSummary(source.getSource(), jumpTarget.getSpan()))
                .setScore(1) // TODO: set a reasonable score
                .setClassType(clazz)
                .setQueryHint(clazz.getHandle().getName())
                .build();
        return hit;
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
