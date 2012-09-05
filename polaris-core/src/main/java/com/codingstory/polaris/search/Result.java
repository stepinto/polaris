package com.codingstory.polaris.search;

import com.codingstory.polaris.parser.Token;
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
    String projectName;
    String filename;
    byte[] fileId;
    String content;
    String summary;
    Explanation explanation;
    Token.Kind kind;

    public int getDocumentId() {
        return documentId;
    }

    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getFileId() {
        return fileId;
    }

    public void setFileId(byte[] fileId) {
        this.fileId = fileId;
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

    public Token.Kind getKind() {
        return kind;
    }

    public void setKind(Token.Kind kind) {
        this.kind = kind;
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
