package com.codingstory.polaris.parser;

import com.codingstory.polaris.parser.ParserProtos.ClassTypeHandle;
import com.codingstory.polaris.parser.ParserProtos.VariableUsage;
import com.codingstory.polaris.parser.ParserProtos.MethodUsage;
import com.codingstory.polaris.parser.ParserProtos.PrimitiveType;
import com.codingstory.polaris.parser.ParserProtos.Type;
import com.codingstory.polaris.parser.ParserProtos.TypeHandle;
import com.codingstory.polaris.parser.ParserProtos.TypeKind;
import com.codingstory.polaris.parser.ParserProtos.JumpTarget;
import com.codingstory.polaris.parser.ParserProtos.TypeUsage;
import com.codingstory.polaris.parser.ParserProtos.Usage;
import com.codingstory.polaris.parser.ParserProtos.Position;
import com.codingstory.polaris.parser.ParserProtos.Span;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import org.apache.commons.lang.StringUtils;

import java.util.Comparator;

public final class TypeUtils {

    private TypeUtils() {}

    public static final Comparator<Position> POSITION_COMPARATOR = new Comparator<Position>() {
        @Override
        public int compare(Position left, Position right) {
            return ComparisonChain.start()
                    .compare(left.getLine(), right.getLine())
                    .compare(left.getColumn(), right.getColumn())
                    .result();
        }
    };

    public static final Comparator<Span> SPAN_COMPARATOR = new Comparator<Span>() {
        @Override
        public int compare(Span left, Span right) {
            return ComparisonChain.start()
                    .compare(left.getFrom(), right.getFrom(), POSITION_COMPARATOR)
                    .compare(left.getTo(), right.getTo(), POSITION_COMPARATOR)
                    .result();
        }
    };

    public static final Comparator<JumpTarget> JUMP_TARGET_COMPARATOR = new Comparator<JumpTarget>() {
        @Override
        public int compare(JumpTarget left, JumpTarget right) {
            return ComparisonChain.start()
                    .compare(left.getFile().getId(), right.getFile().getId())
                    .compare(left.getSpan(), right.getSpan(), SPAN_COMPARATOR)
                    .result();
        }
    };

    public static final Position ZERO_POSITION = positionOf(0, 0);
    public static final Span ZERO_SPAN = spanOf(ZERO_POSITION, ZERO_POSITION);

    public static TypeHandle handleOf(Type type) {
        TypeHandle.Builder builder = TypeHandle.newBuilder()
                .setKind(type.getKind());
        switch (type.getKind()) {
            case PRIMITIVE:
                builder.setPrimitive(type.getPrimitive());
                break;
            case CLASS:
                builder.setClazz(type.getClazz().getHandle());
                break;
            default:
                throw new AssertionError("Unknown kind: " + type.getKind());
        }
        return builder.build();
    }

    /** Gets simple name from a fully qualified name. */
    public static String getSimpleName(String fullName) {
        Preconditions.checkNotNull(fullName);
        int lastDot = fullName.lastIndexOf(".");
        if (lastDot == -1) {
            return fullName;
        }
        return fullName.substring(lastDot + 1);
    }

    /** Gets package from a fully qualified name. */
    public static Object getPackage(String fullName) {
        Preconditions.checkNotNull(fullName);
        int lastDot = fullName.lastIndexOf(fullName);
        if (lastDot == -1) {
            return "";
        }
        return fullName.substring(0, lastDot);
    }

    public static TypeHandle handleOf(PrimitiveType primitive) {
        Preconditions.checkNotNull(primitive);
        return TypeHandle.newBuilder()
                .setKind(TypeKind.PRIMITIVE)
                .setPrimitive(primitive)
                .build();
    }

    public static TypeHandle handleOf(ClassTypeHandle clazz) {
        Preconditions.checkNotNull(clazz);
        return TypeHandle.newBuilder()
                .setKind(TypeKind.CLASS)
                .setClazz(clazz)
                .build();
    }

    public static Usage usageOf(TypeUsage typeUsage, JumpTarget jumpTarget, String snippet) {
        Preconditions.checkNotNull(typeUsage);
        return Usage.newBuilder()
                .setKind(Usage.Kind.TYPE)
                .setType(typeUsage)
                .setJumpTarget(jumpTarget)
                .setSnippet(snippet)
                .build();
    }

    public static Usage usageOf(MethodUsage methodUsage, JumpTarget jumpTarget, String snippet) {
        Preconditions.checkNotNull(methodUsage);
        return Usage.newBuilder()
                .setKind(Usage.Kind.METHOD)
                .setMethod(methodUsage)
                .setJumpTarget(jumpTarget)
                .setSnippet(snippet)
                .build();
    }

    public static Usage usageOf(VariableUsage variableUsage, JumpTarget jumpTarget, String snippet) {
        Preconditions.checkNotNull(variableUsage);
        return Usage.newBuilder()
                .setKind(Usage.Kind.VARIABLE)
                .setVariable(variableUsage)
                .setJumpTarget(jumpTarget)
                .setSnippet(snippet)
                .build();
    }

    public static Position positionOf(int line, int column) {
        return Position.newBuilder()
                .setLine(line)
                .setColumn(column)
                .build();
    }

    public static Span spanOf(Position from, Position to) {
        return Span.newBuilder()
                .setFrom(from)
                .setTo(to) .build();
    }

    public static String snippetLine(String[] lines, JumpTarget jumpTarget) {
        int row = jumpTarget.getSpan().getFrom().getLine();
        String rawSnippet = lines[row];
        return StringUtils.strip(rawSnippet, "\n\t ");
    }
}
