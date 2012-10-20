namespace java com.codingstory.polaris.indexing.layout
namespace py polaris.indexing

enum TLayoutNodeKind {
    FILE = 1,
    DIRECTORY = 2,
}

struct TLayoutNode {
    1: TLayoutNodeKind kind;
    2: string name;
}

struct TLayoutNodeList {
    1: list<TLayoutNode> nodes;
}

