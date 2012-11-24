include "layout.thrift"
include "parser.thrift"

namespace java com.codingstory.polaris.search
namespace py polaris.search

enum TStatusCode {
    OK = 0,
    FILE_NOT_FOUND = -1,
    MISSING_FIELDS = -2,
    NOT_IMPLEMENTED = -3,
    UNKNOWN_ERROR = -99,
}

struct THit {
    1: string project;
    2: string path;
    3: parser.TJumpTarget jumpTarget;
    4: string summary;
    5: double score;
    6: parser.TClassType classType; // if this match is a type
    7: string queryHint; // for query completion
}

struct TSearchRequest {
    1: string query;
    2: i32 rankFrom;
    3: i32 rankTo;
    // TODO: support paging
}

struct TSearchResponse {
    1: TStatusCode status;
    2: list<THit> hits;
    3: i64 latency;
    4: i64 count;
}

struct TSourceRequest {
    1: i64 fileId;
    2: string projectName;
    3: string fileName;
}

struct TSourceResponse {
    1: TStatusCode status;
    2: parser.TSourceFile source;
}

struct TCompleteRequest {
    1: string query;
    2: i32 limit;
}

struct TCompleteResponse {
    1: TStatusCode status;
    2: list<string> entries;
    3: i64 latency;
}

struct TLayoutRequest {
    1: string projectName;
    2: string directoryName;
}

struct TLayoutResponse {
    1: TStatusCode status;
    2: list<string> children;
}

struct TReadClassTypeRequest {
    1: i64 typeId;
}

struct TReadClassTypeResponse {
    1: TStatusCode status;
    2: parser.TClassType classType;
}

struct TListTypeUsagesRequest {
    1: i64 typeId;
}

struct TListTypeUsagesResponse {
    1: TStatusCode status;
    2: list<parser.TTypeUsage> usages;
}

service TCodeSearchService {
    TSearchResponse search(1: TSearchRequest req);
    TSourceResponse source(1: TSourceRequest req);
    TCompleteResponse complete(1: TCompleteRequest req);
    TLayoutResponse layout(1: TLayoutRequest req);
    TReadClassTypeResponse readClassType(1: TReadClassTypeRequest req);
    TListTypeUsagesResponse listTypeUsages(1: TListTypeUsagesRequest req);
}
