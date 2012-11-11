package com.codingstory.polaris.search;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-18
 * Time: 下午11:43
 * To change this template use File | Settings | File Templates.
 */
public class SrcSearcher /*implements Closeable*/ {
    /*
    private static final Log LOGGER = LogFactory.getLog(SrcSearcher.class);
    private static final Set<String> SEARCH_FIELDS = ImmutableSet.of(
            FieldName.DIRECTORY_NAME,
            FieldName.DIRECTORY_LAYOUT,
            FieldName.FIELD_NAME,
            FieldName.FIELD_TYPE_NAME,
            // FieldName.FILE_CONTENT,
            FieldName.FILE_NAME,
            FieldName.FILE_ID,
            FieldName.JAVA_DOC,
            FieldName.KIND,
            FieldName.METHOD_NAME,
            FieldName.OFFSET,
            FieldName.PACKAGE_NAME,
            FieldName.PROJECT_NAME,
            FieldName.SOURCE_ANNOTATIONS,
            FieldName.TYPE_NAME,
            FieldName.TYPE_FULL_NAME_RAW); // just for debug
    private final IndexReader reader;
    private final IndexSearcher searcher;
    private final QueryParser parser;

    public SrcSearcher(IndexReader reader) throws IOException {
        this.reader = Preconditions.checkNotNull(reader);
        searcher = new IndexSearcher(reader);
        String[] fields = SEARCH_FIELDS.toArray(new String[SEARCH_FIELDS.size()]);
        Map<String, Float> boostMap = Maps.newHashMap();
        boostMap.put(FieldName.TYPE_NAME, 4.0f);
        boostMap.put(FieldName.METHOD_NAME, 3.0f);
        boostMap.put(FieldName.PACKAGE_NAME, 1.0f);
        parser = new MultiFieldQueryParser(Version.LUCENE_36, fields, new JavaSrcAnalyzer(), boostMap);
    }

    @Override
    public void close() throws IOException {
        searcher.close();
        reader.close();
    }

    public String getContent(byte[] fileId) throws IOException {
        Query query = new TermQuery(new Term(FILE_ID, Hex.encodeHexString(fileId)));
        int docid = searcher.search(query, 1).scoreDocs[0].doc;
        return new String(Snappy.uncompress(reader.document(docid).getFieldable(FILE_CONTENT).getBinaryValue()));
    }

    public List<TSearchResultEntry> search(String queryString, int from, int to)
            throws ParseException, IOException, InvalidTokenOffsetsException {
        Preconditions.checkNotNull(queryString);
        Preconditions.checkArgument(0 <= from && from <= to);
        LOGGER.debug("Query: " + queryString);
        Query query = parser.parse(queryString);
        TopDocs topDocs = searcher.search(query, to);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        List<TSearchResultEntry> results = Lists.newArrayList();
        Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(), new QueryScorer(query));
        Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_36);
        for (int i = from; i < to && i < scoreDocs.length; i++) {
            TSearchResultEntry result = new TSearchResultEntry();
            int docid = scoreDocs[i].doc;
            Document document = reader.document(docid);
            result.setProjectName(document.getFieldable(PROJECT_NAME).stringValue());
            result.setFileName(document.getFieldable(FILE_NAME).stringValue());
            byte[] fileId = null;
            try {
                fileId = Hex.decodeHex(document.getFieldable(FILE_ID).stringValue().toCharArray());
            } catch (DecoderException e) {
                throw new AssertionError(e);
            }
            result.setFileId(fileId);
            String content = getContent(fileId);
            result.setDocumentId(docid);
            Explanation explanation = searcher.explain(query, docid);
            result.setScore(explanation.getValue());
            result.setExplanation(explanation.toHtml());
            result.setOffset(Long.parseLong(document.get(OFFSET)));
            LOGGER.debug(result.getFileName() + "(" + Hex.encodeHexString(fileId) + ")");
            int offset = Integer.parseInt(document.get(OFFSET));
            result.setSummary(getSummary(content, offset));
            results.add(result);
        }
        return results;
    }

    public String getSummary(String content, int offset) {
        String[] lines = content.split("\n");
        int i = 0;
        int lengthTillNow = 0;
        for (; i < lines.length; ++i) {
            if (lengthTillNow > offset) {
                break;
            }
            lengthTillNow += lines[i].length() + 1;
        }
        StringBuilder builder = new StringBuilder();
        for (int j = i - 2; j < i + 3; ++j) {
            if (j >= 0 && j < lines.length) {
                builder.append(lines[j]);
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    public List<String> complete(final String queryString, int limit) throws IOException {
        LOGGER.debug("Complete query: " + queryString);
        BooleanQuery q = new BooleanQuery();
        q.add(new PrefixQuery(new Term(FieldName.PACKAGE_NAME, queryString)), BooleanClause.Occur.SHOULD);
        q.add(new PrefixQuery(new Term(FieldName.TYPE_NAME, queryString)), BooleanClause.Occur.SHOULD);
        q.add(new PrefixQuery(new Term(FieldName.FILE_NAME, queryString)), BooleanClause.Occur.SHOULD);
        TopDocs topDocs = searcher.search(q, limit);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        List<String> results = Lists.newArrayList();
        for (ScoreDoc scoreDoc : scoreDocs) {
            int docid = scoreDoc.doc;
            Document document = reader.document(docid);
            results.addAll(ImmutableList.copyOf(document.getValues(FieldName.PACKAGE_NAME)));
            results.addAll(ImmutableList.copyOf(document.getValues(FieldName.TYPE_NAME)));
            results.addAll(ImmutableList.copyOf(document.getValues(FieldName.FILE_NAME)));
        }
        results = ImmutableList.copyOf(Iterables.filter(results, new Predicate<String>() {
            @Override
            public boolean apply(String s) {
                return s.toLowerCase().contains(s.toLowerCase());
            }
        }));
        if (results.size() > limit) {
            results = results.subList(0, limit);
        }
        LOGGER.debug("Candidates: " + results);
        return results;
    }*/
}
