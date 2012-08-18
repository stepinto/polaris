package com.codingstory.polaris.indexing.analysis;

import com.codingstory.polaris.parser.JavaTokenExtractor;
import com.codingstory.polaris.parser.Token;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.index.Payload;

import java.io.*;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-18
 * Time: 下午8:30
 * To change this template use File | Settings | File Templates.
 */
public class JavaSrcTokenizer extends Tokenizer {
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);

    private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);

    private final PayloadAttribute payloadAttr = addAttribute(PayloadAttribute.class);

    private final Log LOG = LogFactory.getLog(JavaSrcTokenizer.class);

    private final List<Token> tokens;

    private final Iterator<Token> iterator;

    private final StringBuilder builder = new StringBuilder();

    private final byte[] payloadBytes = new byte[1];

    public static int toInteger(Token.Kind kind) {
        if (kind == Token.Kind.PACKAGE_DECLARATION) {
            return 1;
        } else if (kind == Token.Kind.CLASS_DECLARATION) {
            return 2;
        } else if (kind == Token.Kind.FIELD_DECLARATION) {
            return 3;
        } else if (kind == Token.Kind.METHOD_DECLARATION) {
            return 4;
        }
        return 0;
    }


    public JavaSrcTokenizer(Reader in) throws Exception {
        super(in);
        BufferedReader reader = new BufferedReader(in);
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append('\n');
        }
        JavaTokenExtractor extractor = new JavaTokenExtractor();
        InputStream stream = IOUtils.toInputStream(builder);
        if (stream == null)
            throw new IOException("stream is null");
        extractor.setInputStream(new ByteArrayInputStream(builder.toString().getBytes()));
        try {
            tokens = extractor.extractTokens();
        } catch (Exception e) {
            LOG.fatal("errors occured while parsing " + e.getStackTrace());
            throw e;
        }
        iterator = tokens.iterator();
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();
        if (!iterator.hasNext())
            return false;
        Token t = iterator.next();
        payloadBytes[0] = (byte) toInteger(t.getKind());
        payloadAttr.setPayload(new Payload(payloadBytes));
        int begin = (int) t.getSpan().getFrom();
        int end = (int) t.getSpan().getTo();
        offsetAttr.setOffset(begin, end);
        try {
            termAttr.append(builder.substring(begin, end).toLowerCase());
        } catch (Exception e) {
            LOG.fatal(String.format("out of range, len=%d, begin=%d, end=%d",
                    builder.length(), begin, end));
            termAttr.append("error");
        }
        return true;
    }
}
