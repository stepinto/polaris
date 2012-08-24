package com.codingstory.polaris.search;

import org.apache.lucene.search.Explanation;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-18
 * Time: 下午11:52
 * To change this template use File | Settings | File Templates.
 */
public class Result {
    int documentId;
    String filename;
    String content;
    String summary;
    Explanation explanation;

    public int getDocumentId() {
        return documentId;
    }

    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Explanation getExplanation() {
        return explanation;
    }

    public void setExplanation(Explanation explanation) {
        this.explanation = explanation;
    }

    @Override
    public String toString() {
        return "Result{" +
                "filename='" + filename + '\'' +
                ", content='" + content + '\'' +
                ", summary='" + summary + '\'' +
                '}';
    }
}
