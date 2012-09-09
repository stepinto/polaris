package com.codingstory.polaris.indexing.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-18
 * Time: 下午9:02
 * To change this template use File | Settings | File Templates.
 */
public final class JavaSrcAnalyzer extends Analyzer {
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        try {
            return new JavaSrcTokenizer(reader);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        Analyzer analyzer = new JavaSrcAnalyzer();
        TokenStream stream = analyzer.tokenStream("test", new StringReader("package java.util.ArrayList"));
        while (stream.incrementToken()) {
            String[] parts = stream.reflectAsString(false).split("#");
            for (String s : parts) {
                System.out.println(s);
            }
            System.out.println();
        }
    }
}
