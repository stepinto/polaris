package com.codingstory.polaris.searchui.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SearchResultTransfer implements IsSerializable {

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
