package com.codingstory.polaris;

import com.codingstory.polaris.parser.TJumpTarget;
import com.google.common.base.Preconditions;

public class JumpTarget {
    private final long fileId;
    private final long offset;

    public JumpTarget(long fileId, long offset) {
        Preconditions.checkArgument(offset >= 0);
        this.fileId = IdUtils.checkValid(fileId);
        this.offset = offset;
    }

    public static JumpTarget createFromThrift(TJumpTarget t) {
        Preconditions.checkNotNull(t);
        return new JumpTarget(t.getFileId(), t.getOffset());
    }

    public long getOffset() {
        return offset;
    }

    public long getFileId() {
        return fileId;
    }

    public TJumpTarget toThrift() {
        TJumpTarget t = new TJumpTarget();
        t.setFileId(fileId);
        t.setOffset(offset);
        return t;
    }
}
