package com.codingstory.polaris;

import com.codingstory.polaris.parser.Position;
import com.codingstory.polaris.parser.TJumpTarget;
import com.google.common.base.Preconditions;

public class JumpTarget {
    private final long fileId;
    private final Position position;

    public JumpTarget(long fileId, Position position) {
        Preconditions.checkNotNull(position);
        this.fileId = IdUtils.checkValid(fileId);
        this.position = position;
    }

    public static JumpTarget createFromThrift(TJumpTarget t) {
        Preconditions.checkNotNull(t);
        return new JumpTarget(t.getFileId(), Position.createFromThrift(t.getPosition()));
    }

    public Position getPosition() {
        return position;
    }

    public long getFileId() {
        return fileId;
    }

    public TJumpTarget toThrift() {
        TJumpTarget t = new TJumpTarget();
        t.setFileId(fileId);
        t.setPosition(position.toThrift());
        return t;
    }
}
