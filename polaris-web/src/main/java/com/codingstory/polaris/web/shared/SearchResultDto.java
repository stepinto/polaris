package com.codingstory.polaris.web.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

public class SearchResultDto implements IsSerializable {
    public static class Entry implements IsSerializable {
        public String fileName;
        public String summary;

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
