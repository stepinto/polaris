package com.codingstory.polaris.search;

import com.codingstory.polaris.JumpTarget;
import com.codingstory.polaris.parser.ClassType;
import com.codingstory.polaris.parser.SourceFile;
import com.codingstory.polaris.parser.Span;
import com.codingstory.polaris.sourcedb.SourceDb;
import com.codingstory.polaris.typedb.TypeDb;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.List;

/**
 * Mixes and re-ranks search results from {@link com.codingstory.polaris.typedb.TypeDb}
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

    public List<THit> search(String query, int n) throws IOException {
        Preconditions.checkNotNull(query);
        Preconditions.checkArgument(n >= 0);
        LOG.info("Query: " + query);
        // TODO: Need to understand query
        List<THit> result = Lists.newArrayList();
        List<ClassType> classTypes = typeDb.completeQuery(query, n); // TODO: Use dedicated approach
        for (ClassType classType : classTypes) {
            result.add(classTypeToHit(classType));
        }
        return result;
    }

    public List<THit> complete(String query, int n) throws IOException {
        Preconditions.checkNotNull(query);
        Preconditions.checkArgument(n >= 0);
        LOG.info("Complete query: " + query);
        List<THit> result = Lists.newArrayList();
        List<ClassType> classTypes = typeDb.completeQuery(query, n);
        for (ClassType classType : classTypes) {
            result.add(classTypeToHit(classType));
        }
        return result;
    }

    private THit classTypeToHit(ClassType type) throws IOException {
        THit hit = new THit();
        JumpTarget jumpTarget = type.getJumpTarget();
        SourceFile source = sourceDb.querySourceById(jumpTarget.getFile().getId());
        hit.setProject(source.getProject());
        hit.setPath(source.getPath());
        hit.setJumpTarget(jumpTarget.toThrift());
        hit.setSummary(getSummary(source.getSource(), jumpTarget.getSpan()));
        hit.setScore(1); // TODO: set a reasonable score
        hit.setClassType(type.toThrift());
        hit.setQueryHint(type.getName().toString());
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
