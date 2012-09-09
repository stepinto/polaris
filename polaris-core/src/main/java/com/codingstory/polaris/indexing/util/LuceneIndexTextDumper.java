package com.codingstory.polaris.indexing.util;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-9-8
 * Time: 下午4:50
 * To change this template use File | Settings | File Templates.
 */
public class LuceneIndexTextDumper {
    private void dumpFieldable(Fieldable field, PrintStream stream) {
        stream.print("#field " + field.name() + ":");
        if (field.stringValue() != null) {
            stream.println(field.stringValue().replace('\n', ' ').replace("\r", ""));
        } else {
            stream.println();
        }
    }

    private void dumpDocuments(IndexReader reader, PrintStream stream) throws IOException {
        for (int i = 0; i < reader.maxDoc(); ++i) {
            if (reader.isDeleted(i))
                continue;
            Document doc = reader.document(i);
            stream.println("id:" + i);
            for (Fieldable field : doc.getFields()) {
                dumpFieldable(field, stream);
            }
        }
    }

    private void dumpTerms(IndexReader reader, PrintStream stream) throws IOException {
        TermEnum termEnum = reader.terms();
        while (termEnum.next()) {
            Term term = termEnum.term();
            stream.print("term: " + term.field() + ":" + term.text());
            TermDocs termDocs = reader.termDocs(term);
            while (termDocs.next()) {
                stream.print(" ");
                stream.print(termDocs.doc() + ":" + termDocs.freq());
            }
            stream.println();
        }
    }

    public void dumpIndex(File indexDir, PrintStream stream) throws IOException {
        IndexReader reader = IndexReader.open(FSDirectory.open(indexDir));
        stream.println("###DOCUMENTS: ");
        dumpDocuments(reader, stream);
        stream.println("###TERMS: ");
        dumpTerms(reader, stream);
        reader.close();
    }

    public static void main(String[] args) throws Exception {
        LuceneIndexTextDumper dumper = new LuceneIndexTextDumper();
        dumper.dumpIndex(new File("index"), System.out);
    }
}
