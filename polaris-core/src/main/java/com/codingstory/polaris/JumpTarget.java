package com.codingstory.polaris;

import com.codingstory.polaris.parser.Span;
import com.codingstory.polaris.parser.TJumpTarget;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public final class JumpTarget {
    private final long fileId;
    private final Span span;

    public JumpTarget(long fileId, Span span) {
        this.fileId = IdUtils.checkValid(fileId);
        this.span = Preconditions.checkNotNull(span);
    }

    public static JumpTarget createFromThrift(TJumpTarget t) {
        Preconditions.checkNotNull(t);
        return new JumpTarget(t.getFileId(), Span.createFromThrift(t.getSpan()));
    }

    public Span getSpan() {
        return span;
    }

    public long getFileId() {
        return fileId;
    }

    public TJumpTarget toThrift() {
        TJumpTarget t = new TJumpTarget();
        t.setFileId(fileId);
        t.setSpan(span.toThrift());
        return t;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o.getClass() != JumpTarget.class) {
            return false;
        }

        JumpTarget that = (JumpTarget) o;
        return Objects.equal(this.fileId, that.fileId) && Objects.equal(this.span, that.span);
    }

    @Override
    public int hashCode() {
        int result = (int) (fileId ^ (fileId >>> 32));
        result = 31 * result + (span != null ? span.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("fileId", fileId)
                .add("span", span)
                .toString();
    }
}
