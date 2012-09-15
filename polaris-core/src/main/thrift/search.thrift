include "token.thrift"
namespace java com.codingstory.polaris.search

enum TStatusCode {
    OK = 0,
    FILE_NOT_FOUND = -1,
    UNKNOWN_ERROR = -99,
}

struct TSearchResultEntry {
    1: string projectName;
    2: string fileName;
    3: string fileId;
    4: string summary;
    5: string explanation;
    6: double score;
    7: token.TTokenKind kind;
    8: i64 documentId;
}

struct TSearchRequest {
    1: string query;
    // TODO: support paging
}

struct TSearchResponse {
    1: TStatusCode status;
    2: list<TSearchResultEntry> entries;
    3: i64 latency;
    4: i64 count;
}

struct TSourceRequest {
    1: string fileId;
}

struct TSourceResponse {
    1: TStatusCode status;
    2: string projectName;
    3: string fileName;
    4: string content;
    5: list<token.TToken> tokens;
}

struct TCompleteRequest {
    1: string query;
    2: i32 limit;
}

struct TCompleteResponse {
    1: TStatusCode status;
    2: list<string> entries;
}

service TCodeSearchService {
    TSearchResponse search(1: TSearchRequest req);
    TSourceResponse source(1: TSourceRequest req);
    TCompleteResponse complete(1: TCompleteRequest req);
}
