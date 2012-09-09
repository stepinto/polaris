namespace java com.codingstory.polaris.indexing

enum TTokenKind {
    CLASS_DECLARATION = 1,
    INTERFACE_DECLARATION = 2,
    ENUM_DECLARATION = 3,
    ANNOTATION_DECLARATION = 4,
}

struct TTypeDeclaration {
    1: string name;
}

struct TTokenSpan {
    1: i64 from;
    2: i64 to;
}

struct TToken {
    1: TTokenKind kind;
    2: TTokenSpan span;
    3: TTypeDeclaration typeDeclaration;
}

struct TTokenList {
    1: list<TToken> tokens;
}
