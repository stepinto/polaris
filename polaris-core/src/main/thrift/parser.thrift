namespace java com.codingstory.polaris.parser
namespace py polaris.parser

struct TPosition {
    1: i32 line;
    2: i32 column;
}

struct TSpan {
    1: TPosition from;
    2: TPosition to;
}

struct TFileHandle {
    1: i64 id;
    2: string project;
    3: string path;
}

struct TJumpTarget {
    4: TFileHandle file;
    3: TSpan span;
    // OBSOLETED 1: i64 fileId;
    // OBSOLETED 2: TPosition position;
}

struct TTypeHandle {
    1: i64 id;
    2: string name;
}

struct TFieldHandle {
    1: i64 id;
    2: string name;
}

struct TMethodHandle {
    1: i64 id;
    2: string name;
    3: list<TTypeHandle> parameters;
}

struct TField {
    1: TFieldHandle handle;
    2: TTypeHandle type;
    3: TJumpTarget jumpTarget;
    // modifiers
}

struct TMethodParameter {
    1: string name;
    2: TTypeHandle type;
}

struct TMethod {
    1: TMethodHandle handle;
    2: TTypeHandle returnType;
    3: list<TMethodParameter> parameters;
    4: list<TTypeHandle> exceptions;
    5: TJumpTarget jumpTarget;
}

enum TClassTypeKind {
    CLASS = 1;
    INTERFACE = 2;
    ENUM = 3;
    ANNOTATION = 4;
}

struct TClassType {
    1: TTypeHandle handle;
    2: TClassTypeKind kind;
    3: list<TTypeHandle> superTypes;
    4: list<TField> fields;
    5: list<TMethod> methods;
    6: string javaDoc;
    7: TJumpTarget jumpTarget;
}

enum TTypeUsageKind {
    IMPORT = 1,
    SUPER_CLASS = 2,
    METHOD_SIGNATURE = 3,
    FIELD = 4,
    LOCAL_VARIABLE = 5,
    GENERIC_TYPE_PARAMETER = 6,
    TYPE_DECLARATION = 7,
}

struct TTypeUsage {
    1: TTypeHandle type;
    4: TJumpTarget jumpTarget;
    3: TTypeUsageKind kind;
    // OBSOLETED 2: TSpan span;
}

struct TUsage {
    // Exactly one of below appears.
    1: TTypeUsage typeUsage;
}

enum TFieldUsageKind {
    FIELD_DECLARATION,
    FIELD_ACCESS,
}

struct TFieldUsage {
    1: TFieldHandle field;
    4: TJumpTarget jumpTarget;
    3: TFieldUsageKind kind;
    // OBSOLETED: 2: TSpan span;
}

enum TMethodUsageKind {
    METHOD_DECLARATION = 1,
    METHOD_CALL = 2,
}

struct TMethodUsage {
    1: TMethodHandle method;
    4: TJumpTarget jumpTarget;
    3: TMethodUsageKind kind;
    // OBSOLETED 2: TSpan span;
}

struct TSourceFile {
    6: TFileHandle handle;
    4: string source;
    5: string annotatedSource;
}
