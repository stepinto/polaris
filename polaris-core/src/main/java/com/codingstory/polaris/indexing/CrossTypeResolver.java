package com.codingstory.polaris.indexing;

import com.codingstory.polaris.parser.FullyQualifiedTypeName;
import com.codingstory.polaris.parser.ResolvedTypeReference;
import com.codingstory.polaris.parser.Token;
import com.codingstory.polaris.parser.TypeResolver;
import com.codingstory.polaris.parser.UnresolvedTypeReferenece;
import com.google.common.base.Preconditions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;

public class CrossTypeResolver implements TypeResolver {

    private static final Log LOGGER = LogFactory.getLog(CrossTypeResolver.class);
    private final IndexSearcher searcher;

    public CrossTypeResolver(IndexSearcher searcher) {
        this.searcher = Preconditions.checkNotNull(searcher);
    }

    @Override
    public ResolvedTypeReference resolve(UnresolvedTypeReferenece unresolved) throws IOException {
        Preconditions.checkNotNull(unresolved);
        for (FullyQualifiedTypeName candidate : unresolved.getCandidates()) {
            if (checkCandidate(candidate)) {
                ResolvedTypeReference resolved = new ResolvedTypeReference(candidate);
                LOGGER.debug(String.format("Resolved %s as %s (cross reference)",
                        unresolved.getUnqualifiedName(),
                        resolved.getName()));
                return resolved;
            }
        }
        return null;
    }

    private boolean checkCandidate(FullyQualifiedTypeName candidate) throws IOException {
        BooleanQuery query = new BooleanQuery();
        String typeName = candidate.toString().toLowerCase(); // TODO: why need toLowerCase()?
        query.add(new TermQuery(new Term(FieldName.TYPE_NAME, typeName)), BooleanClause.Occur.MUST);
        BooleanQuery subquery = new BooleanQuery();
        subquery.add(whereKindIs(Token.Kind.CLASS_DECLARATION), BooleanClause.Occur.SHOULD);
        subquery.add(whereKindIs(Token.Kind.INTERFACE_DECLARATION), BooleanClause.Occur.SHOULD);
        subquery.add(whereKindIs(Token.Kind.ENUM_DECLARATION), BooleanClause.Occur.SHOULD);
        subquery.add(whereKindIs(Token.Kind.ANNOTATION_DECLARATION), BooleanClause.Occur.SHOULD);
        query.add(subquery, BooleanClause.Occur.MUST);
        TopDocs topDocs = searcher.search(query, 1);
        return topDocs.scoreDocs.length > 0;
    }

    private static Query whereKindIs(Token.Kind kind) {
        return new TermQuery(new Term(FieldName.KIND, String.valueOf(kind.ordinal())));
    }
}
