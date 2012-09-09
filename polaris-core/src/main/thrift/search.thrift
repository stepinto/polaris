include "token.thrift"
namespace java com.codingstory.polaris.search

enum TStatusCode {
    OK = 0,
    FILE_NOT_FOUND = -1,
    UNKNOWN_ERROR = -99,
}

struct TSourceRequest {
    1: binary fileId;
}

struct TSourceResponse {
    1: TStatusCode status,
    2: string projectName;
    3: string fileName;
    4: binary content;
    5: list<token.TToken> tokens;
}

service TCodeSearchService {
    TSourceResponse source(1: TSourceRequest req);
}
