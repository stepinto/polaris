package com.codingstory.polaris.indexing.analysis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-18
 * Time: 下午8:30
 * To change this template use File | Settings | File Templates.
 */
public final class JavaSrcTokenizer extends Tokenizer {
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);

    private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);

    private final Log LOG = LogFactory.getLog(JavaSrcTokenizer.class);

    private final StringBuilder builder = new StringBuilder();

    private final List<String> tokens = new ArrayList<String>();

    private final List<Integer> offsets = new ArrayList<Integer>();

    private int index = 0;

    public JavaSrcTokenizer(Reader in) throws Exception {
        super(in);
        BufferedReader reader = new BufferedReader(in);
        String content = reader.readLine();
        String[] partsBySpace = content.split("\\s+");
        int offset = 0;
        for (String partBySpace : partsBySpace) {
            String[] partsByDot = partBySpace.split("\\.");
            if (partsByDot.length > 1) {
                tokens.add(partBySpace);
                offsets.add(offset);
            }
            int inOffset = offset;
            for (String partByDot : partsByDot) {
                boolean flag = false;
                for (char ch : partByDot.toCharArray()) {
                    if (Character.isUpperCase(ch))
                        flag = true;
                }
                if (!flag) {
                    tokens.add(partByDot);
                    offsets.add(inOffset);
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                int inOffset1 = inOffset;
                for (char ch : partByDot.toCharArray()) {
                    if (Character.isUpperCase(ch)) {
                        if (builder.length() > 0) {
                            tokens.add(builder.toString());
                            offsets.add(inOffset1);
                            builder = new StringBuilder();
                        }
                    }
                    builder.append(ch);
                    ++inOffset1;
                }
                if (builder.length() > 0) {
                    tokens.add(builder.toString());
                    offsets.add(inOffset1);
                    builder = new StringBuilder();
                }
                inOffset += partByDot.length() + 1;
            }
            offset += partBySpace.length() + 1;
        }
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();
        if (index >= tokens.size())
            return false;
        termAttr.append(tokens.get(index).toLowerCase());
        offsetAttr.setOffset(offsets.get(index), offsets.get(index) + tokens.get(index).length());
        ++index;
        return true;
    }
}
