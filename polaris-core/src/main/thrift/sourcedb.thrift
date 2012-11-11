include "parser.thrift"
namespace java com.codingstory.polaris.sourcedb

struct TSourceData {
    1: parser.TSourceFile sourceFile;
}
