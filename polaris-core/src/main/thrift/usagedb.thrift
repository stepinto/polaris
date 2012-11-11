include "parser.thrift"
namespace java com.codingstory.polaris.usagedb

struct TUsageData {
    1: parser.TTypeUsage typeUsage;
    // TODO: TMethodUsage, TFieldUsage
}
