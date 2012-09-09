package com.codingstory.polaris.indexing.analysis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-17
 * Time: 下午3:35
 * To change this template use File | Settings | File Templates.
 */
public final class SimpleClassNameTokenizer extends Tokenizer {

    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);

    private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);

    //private final PayloadAttribute payloadAttr = addAttribute(PayloadAttribute.class);

    private final Matcher matcher;

    private final Log LOG = LogFactory.getLog(SimpleClassNameTokenizer.class);

    public SimpleClassNameTokenizer(Reader in) throws IOException {
        super(in);
        BufferedReader reader = new BufferedReader(in);
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append('\n');
        }
        Pattern p = Pattern.compile("class\\s+([a-zA-Z]+)");
        matcher = p.matcher(builder);
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();
        if (matcher.find()) {
            String match = matcher.group(1);
            LOG.info("analyzed " + match);
            termAttr.append(match);
            offsetAttr.setOffset(matcher.start(1), matcher.end(1));
            return true;
        }
        return false;
    }
}
