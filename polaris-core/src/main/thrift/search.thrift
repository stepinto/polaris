include "parser.thrift"

namespace java com.codingstory.polaris.search
namespace py polaris.search

enum TStatusCode {
    OK = 0,
    FILE_NOT_FOUND = 1001,
    MISSING_FIELDS = 1002,
    NOT_IMPLEMENTED = 1003,
    NOT_UNIQUE = 1004,
    UNKNOWN_ERROR = 1099,
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
    4: list<THit> hits;
    3: i64 latency;
    // OBSOLETED 2: list<string> entries;
}

struct TLayoutRequest {
    1: string projectName;
    2: string directoryName;
}

struct TLayoutResponse {
    1: TStatusCode status;
    3: list<string> directories;
    4: list<parser.TFileHandle> files;
}

struct TGetTypeRequest {
    // If "typeId" is specified, "project" and "typeName" are ignored.
    1: i64 typeId;
    2: string project; // search in all projects if empty
    3: string typeName; // full name
}

struct TGetTypeResponse {
    1: TStatusCode status;
    2: parser.TClassType classType;
}

struct TListTypesInFileRequest {
    1: i64 fileId;
    2: i32 limit;
}

struct TListTypesInFileResponse {
    1: TStatusCode status;
    2: list<parser.TClassType> classTypes;
}

struct TListTypeUsagesRequest {
    1: i64 typeId;
}

struct TListTypeUsagesResponse {
    1: TStatusCode status;
    2: list<parser.TTypeUsage> usages;
}

struct TGetFieldRequest {
    1: i64 fieldId;
}

struct TGetFieldResponse {
    1: TStatusCode status;
    2: parser.TField field;
}

struct TGetMethodRequest {
    1: i64 methodId;
}

struct TGetMethodResponse {
    1: TStatusCode status;
    2: parser.TMethod method;
}

service TCodeSearchService {
    TSearchResponse search(1: TSearchRequest req);
    TSourceResponse source(1: TSourceRequest req);
    TCompleteResponse complete(1: TCompleteRequest req);
    TLayoutResponse layout(1: TLayoutRequest req);
    TGetTypeResponse getType(1: TGetTypeRequest req);
    TListTypesInFileResponse listTypesInFile(1: TListTypesInFileRequest req);
    TListTypeUsagesResponse listTypeUsages(1: TListTypeUsagesRequest req);
    TGetFieldResponse getField(1: TGetFieldRequest req);
    TGetMethodResponse getMethod(1: TGetMethodRequest req);
}
