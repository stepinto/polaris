namespace java com.codingstory.polaris.indexing.layout
namespace py polaris.layout

enum TNodeKind {
    FILE = 1,
    DIRECTORY = 2,
}

struct TChildNode {
    1: binary id;
    2: string name;
    3: TNodeKind kind;
}

struct TDirectoryNode {
    1: binary id;
    2: string name;
    4: list<TChildNode> children;
}

