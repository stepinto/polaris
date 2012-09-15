namespace java com.codingstory.polaris.indexing

enum TTokenKind {
    PACKAGE_DECLARATION = 7,
    CLASS_DECLARATION = 1,
    INTERFACE_DECLARATION = 2,
    ENUM_DECLARATION = 3,
    ANNOTATION_DECLARATION = 4,
    FIELD_DECLARATION = 5,
    METHOD_DECLARATION = 8,
    TYPE_USAGE = 6,
}

struct TTypeDeclaration {
    1: string name;
}

struct TTypeReference {
    1: bool resolved;
    2: list<string> candidates;
}

struct TFieldDeclaration {
    1: string name;
    2: TTypeReference typeReference;
}

struct TTypeUsage {
    1: TTypeReference typeReference;
}

struct TTokenSpan {
    1: i64 from;
    2: i64 to;
}

struct TToken {
    1: TTokenKind kind;
    2: TTokenSpan span;
    3: TTypeDeclaration typeDeclaration;
    4: TFieldDeclaration fieldDeclaration;
    5: TTypeUsage typeUsage;
}

struct TTokenList {
    1: list<TToken> tokens;
}
