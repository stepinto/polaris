package com.codingstory.polaris;

import com.codingstory.polaris.parser.FileHandle;
import com.codingstory.polaris.parser.Span;
import com.codingstory.polaris.parser.TJumpTarget;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public final class JumpTarget {
    private final FileHandle file;
    private final Span span;

    public JumpTarget(FileHandle file, Span span) {
        this.file = Preconditions.checkNotNull(file);
        this.span = Preconditions.checkNotNull(span);
    }

    public static JumpTarget createFromThrift(TJumpTarget t) {
        Preconditions.checkNotNull(t);
        return new JumpTarget(
                FileHandle.createFromThrift(t.getFile()),
                Span.createFromThrift(t.getSpan()));
    }

    public Span getSpan() {
        return span;
    }

    public FileHandle getFile() {
        return file;
    }

    public TJumpTarget toThrift() {
        TJumpTarget t = new TJumpTarget();
        t.setFile(file.toThrift());
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
        return Objects.equal(this.file, that.file) && Objects.equal(this.span, that.span);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(file, span);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("file", file)
                .add("span", span)
                .toString();
    }
}
