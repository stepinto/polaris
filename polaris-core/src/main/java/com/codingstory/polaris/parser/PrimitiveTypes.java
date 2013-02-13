package com.codingstory.polaris.parser;

import com.codingstory.polaris.parser.ParserProtos.PrimitiveType;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public final class PrimitiveTypes {

    public static PrimitiveType BYTE = buildPrimitiveTypeOfKind(PrimitiveType.Kind.BYTE);
    public static PrimitiveType SHORT = buildPrimitiveTypeOfKind(PrimitiveType.Kind.SHORT);
    public static PrimitiveType INTEGER = buildPrimitiveTypeOfKind(PrimitiveType.Kind.INTEGER);
    public static PrimitiveType LONG = buildPrimitiveTypeOfKind(PrimitiveType.Kind.LONG);
    public static PrimitiveType FLOAT = buildPrimitiveTypeOfKind(PrimitiveType.Kind.FLOAT);
    public static PrimitiveType DOUBLE = buildPrimitiveTypeOfKind(PrimitiveType.Kind.DOUBLE);
    public static PrimitiveType VOID = buildPrimitiveTypeOfKind(PrimitiveType.Kind.VOID);
    private static final Map<String, PrimitiveType> PRIMITIVE_TYPES;

    static {
        ImmutableMap.Builder<String, PrimitiveType> builder = ImmutableMap.builder();
        builder.put("byte", BYTE);
        builder.put("short", SHORT);
        builder.put("int", INTEGER);

        builder.put("float", FLOAT);
        builder.put("double", DOUBLE);
        builder.put("void", VOID);
        PRIMITIVE_TYPES = builder.build();
    }

    private PrimitiveTypes() {}

    public static PrimitiveType parse(String name) {
        return PRIMITIVE_TYPES.get(name);
    }

    private static PrimitiveType buildPrimitiveTypeOfKind(PrimitiveType.Kind kind) {
        return PrimitiveType.newBuilder()
                .setKind(kind)
                .build();
    }
}
