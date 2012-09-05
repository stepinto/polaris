package com.codingstory.polaris.web.shared;

import com.codingstory.polaris.parser.Token;
import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

public class SearchResultDto implements IsSerializable {
    public static class Entry implements IsSerializable {
        private String projectName;
        private String fileName;
        private String summary;
        private String explanation;
        private double score;
        private Token.Kind kind;

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public Token.Kind getKind() {
            return kind;
        }

        public void setKind(Token.Kind kind) {
            this.kind = kind;
        }
    }

    private List<Entry> entries;
    private long latency;

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public long getLatency() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }
}
