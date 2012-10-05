package com.codingstory.polaris.web.client.stub;

import com.codingstory.polaris.web.client.NativeHelper;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.http.client.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CodeSearchStub {

    public static class StatusCode {

        public static final StatusCode OK = new StatusCode(0);
        public static final StatusCode FILE_NOT_FOUND = new StatusCode(-1);
        public static final StatusCode UNKNOWN_ERROR = new StatusCode(-99);

        private static final Set<StatusCode> VALUES = ImmutableSet.of(
                OK, FILE_NOT_FOUND, UNKNOWN_ERROR);
        private static final Map<Integer, StatusCode> INTEGER_TO_STATUS_INDEX = Maps.uniqueIndex(VALUES,
                new Function<StatusCode, Integer>() {
            @Override
            public Integer apply(StatusCode statusCode) {
                return statusCode.getValue();
            }
        });

        private int code;

        private StatusCode(int code) {
            this.code = code;
        }

        public int getValue() { return code; }

        @Override
        public String toString() { return String.valueOf(code); }

        public static StatusCode of(int code) {
            return INTEGER_TO_STATUS_INDEX.get(code);
        }
    }

    public static final class TokenSpan extends JavaScriptObject {
        protected TokenSpan() {};

        public native int getFrom() /*-{ return this.from; }-*/;
        public native int getTo() /*-{ return this.to; }-*/;
    }

    public static final class Token extends JavaScriptObject {
        protected Token() {}

        public native TokenSpan getSpan() /*-{ return this.span; }-*/;
        public TokenKind getKind() { return TokenKind.of(doGetKind()); }
        private native int doGetKind() /*-{ return this.kind; }-*/;
        public native TypeDeclaration getTypeDeclaration() /*-{ return this.typeDeclaration; }-*/;
        public native boolean hasTypeDeclaration() /*-{ return this.typeDeclaration != undefined; }-*/;
        public native FieldDeclaration getFieldDeclaration() /*-{ return this.fieldDeclaration; }-*/;
        public native boolean hasFieldDeclaration() /*-{ return this.fieldDeclaration != undefined; }-*/;
        public native TypeUsage getTypeUsage() /*-{ return this.typeUsage; }-*/;
        public native boolean hasTypeUsage() /*-{ return this.typeUsage != undefined; }-*/;
    }

    public static enum TokenKind {
        PACKAGE_DECLARATION,
        CLASS_DECLARATION,
        ENUM_DECLARATION,
        ANNOTATION_DECLARATION,
        INTERFACE_DECLARATION,
        METHOD_DECLARATION,
        FIELD_DECLARATION,
        TYPE_USAGE;

        private static Map<Integer, TokenKind> VALUE_TO_ENUM_MAP = Maps.uniqueIndex(
                ImmutableList.copyOf(values()),
                new Function<TokenKind, Integer>() {
            @Override
            public Integer apply(TokenKind tokenKind) {
                return tokenKind.ordinal();
            }
        });

        public static TokenKind of(int value) {
            return VALUE_TO_ENUM_MAP.get(value);
        }
    }

    public static final class TypeDeclaration extends JavaScriptObject {
        protected  TypeDeclaration() {}

        public native String getName() /*-{ return this.name }-*/;
    }

    public static final class FieldDeclaration extends JavaScriptObject {
        protected FieldDeclaration() {}

        public native String getName() /*-{ return this.name }-*/;
        public native TypeReference getTypeRefernece() /*-{ return this.typeReference }-*/;
    }

    public static final class TypeUsage extends JavaScriptObject {
        protected TypeUsage() {}

        public native TypeReference getTypeRefernece() /*-{ return this.typeReference }-*/;
    }

    public static final class TypeReference extends JavaScriptObject {
        protected TypeReference() {}

        public boolean isResolved() { return doGetResolved() == 1; }
        private native int doGetResolved() /*-{ return this.resolved; }-*/;
        public List<String> getCandidates() { return jsArrayStringToList(doGetCandidates()); }

        public native JsArrayString doGetCandidates() /*-{ return this.candidates }-*/;
    }

    public static class SearchRequest {
        private String query;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }

    public final static class SearchResponse extends JavaScriptObject {
        public final static class Entry extends JavaScriptObject {
            protected Entry() {}

            public native String getProjectName() /*-{ return this.projectName; }-*/;
            public native String getFileName() /*-{ return this.fileName; }-*/;
            public native String getFileId() /*-{ return this.fileId; }-*/;
            public native String getSummary() /*-{ return this.summary; }-*/;
            public native String getExplanation() /*-{ return this.explanation; }-*/;
            public native double getScore() /*-{ return this.score; }-*/;
            public TokenKind getKind() { return TokenKind.of(doGetKind()); }
            private native int doGetKind() /*-{ return this.kind; }-*/;
            // TODO: public long getDocumentId() {}
            public native int getOffset() /*-{ return this.offset; }-*/;
        }

        protected SearchResponse() {}

        public StatusCode getStatus() {
            return StatusCode.of(doGetStatus());
        }

        private native int doGetStatus() /*-{ return this.status; }-*/;

        public List<Entry> getEntries() {
            JsArray<Entry> jsa = doGetEntries();
            List<Entry> result = Lists.newArrayList();
            for (int i = 0; i < jsa.length(); i++) {
                result.add(jsa.get(i));
            }
            return result;
        }

        private native JsArray<Entry> doGetEntries() /*-{ return this.entries; }-*/;
        public native int getLatency() /*-{ return this.latency; }-*/;
    }

    public static class SourceRequest {
        private String fileId;

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }
    }

    public static final class SourceResponse extends JavaScriptObject {
        protected SourceResponse() {}

        public StatusCode getStatus() {
            return StatusCode.of(doGetStatus());
        }

        private native int doGetStatus() /*-{
            return this.status;
        }-*/;

        public native String getProjectName() /*-{

        }-*/;

        public native String getFileName() /*-{
            return this.fileName;
        }-*/;

        public native String getContent() /*-{ return this.content; }-*/;
        public List<Token> getTokens() { return jsArrayToList(doGetTokens()); }
        private native JsArray<Token> doGetTokens() /*-{ return this.tokens; }-*/;
    }

    public static class CompleteRequest {
        private String query;
        private int limit;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }
    }

    public static final class CompleteResponse extends JavaScriptObject {
        protected CompleteResponse() {}

        public StatusCode getStatus() { return StatusCode.of(doGetStatus()); }
        private native int doGetStatus() /*-{ return this.status; }-*/;
        public List<String> getEntries() { return jsArrayStringToList(doGetEntries()); }
        private native JsArrayString doGetEntries() /*-{ return this.entries; }-*/;
    }

    public static void search(SearchRequest req, Callback<SearchResponse, Throwable> callback) {
        Preconditions.checkNotNull(req);
        Preconditions.checkNotNull(callback);
        String query = req.getQuery();
        call("search", "q=" + URL.encode(query), new Function<String, SearchResponse>() {
            @Override
            public SearchResponse apply(String s) {
                return (SearchResponse) NativeHelper.parseSafeJson(s);
            }
        }, callback);
    }

    public static void source(SourceRequest req, Callback<SourceResponse, Throwable> callback) {
        Preconditions.checkNotNull(req);
        Preconditions.checkNotNull(callback);
        String fileId = req.getFileId();
        call("source", "f=" + URL.encode(fileId), new Function<String, SourceResponse>() {
            @Override
            public SourceResponse apply(String s) {
                return (SourceResponse) NativeHelper.parseSafeJson(s);
            }
        }, callback);
    }

    public static void complete(CompleteRequest req, Callback<CompleteResponse, Throwable> callback) {
        Preconditions.checkNotNull(req);
        Preconditions.checkNotNull(callback);
        call("complete", "q=" + URL.encode(req.getQuery()) + "&n=" + req.getLimit(),
                new Function<String, CompleteResponse>() {
            @Override
            public CompleteResponse apply(String s) {
                return (CompleteResponse) NativeHelper.parseSafeJson(s);
            }
        }, callback);
    }

    private static <R> void call(
            String method,
            String body,
            final Function<String, R> translator,
            final Callback<R, Throwable> callback) {
        try {
            // TODO: Don't know why post doesn't work.
            RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET,
                    "/ajax/" + URL.encode(method) + "?" + URL.encode(body));
            requestBuilder.sendRequest(body, new RequestCallback() {
                @Override
                public void onResponseReceived(Request req, Response resp) {
                    callback.onSuccess(translator.apply(resp.getText()));
                }

                @Override
                public void onError(Request req, Throwable e) {
                    callback.onFailure(e);
                }
            });
        } catch (RequestException e) {
            callback.onFailure(e);
        }
    }

    private static <T extends JavaScriptObject> List<T> jsArrayToList(JsArray<T> a) {
        List<T> results = Lists.newArrayList();
        for (int i = 0; i < a.length(); i++) {
            results.add(a.get(i));
        }
        return results;
    }

    private static List<String> jsArrayStringToList(JsArrayString a) {
        List<String> results = Lists.newArrayList();
        for (int i = 0; i < a.length(); i++) {
            results.add(a.get(i));
        }
        return results;
    }

}
