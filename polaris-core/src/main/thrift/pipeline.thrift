include "parser.thrift"

namespace java com.codingstory.polaris.pipeline

struct TFileContent {
    1: parser.TFileHandle file;
    2: string content;
}

struct TParsedFile {
    3: string package_;
    2: list<parser.TClassType> classes; // valid after 1st pass
    6: parser.TSourceFile source; // valid after 2nd pass
    4: list<parser.TTypeUsage> typeUsages; // valid after 2nd pass
}

struct TFileImports {
    // "source" imports "target"
    1: parser.TFileHandle file;
    2: string package_;
    3: list<string> importedClasses;
    4: list<string> importedPackages;
}

