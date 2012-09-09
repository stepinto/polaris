package com.codingstory.polaris.indexing;

import com.codingstory.polaris.parser.Token;
import com.codingstory.polaris.parser.TypeDeclaration;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 * Converters from POJOs to Thrift objects.
 */
public final class PojoToThriftConverter {
    public static Function<TypeDeclaration, TToken> TYPE_DECLARATION_TO_TTOKEN_CONVERTER =
            new Function<TypeDeclaration, TToken>() {
                @Override
                public TToken apply(TypeDeclaration typeDeclaration) {
                    if (typeDeclaration == null) {
                        return null;
                    }
                    TToken result = convertTokenCommon(typeDeclaration);
                    result.setTypeDeclaration(convertTypeDeclaration(typeDeclaration));
                    return result;
                }

                private TTypeDeclaration convertTypeDeclaration(TypeDeclaration typeDeclaration) {
                    TTypeDeclaration result = new TTypeDeclaration();
                    result.setName(typeDeclaration.getName().toString());
                    return result;
                }
            };

    private static TToken convertTokenCommon(Token token) {
        Preconditions.checkNotNull(token);
        TToken result = new TToken();
        result.setKind(convertTokenKind(token.getKind()));
        result.setSpan(convertTokenSpan(token.getSpan()));
        return result;
    }

    private static TTokenKind convertTokenKind(Token.Kind kind) {
        Preconditions.checkNotNull(kind);
        switch (kind) {
            case CLASS_DECLARATION:
                return TTokenKind.CLASS_DECLARATION;
            case INTERFACE_DECLARATION:
                return TTokenKind.INTERFACE_DECLARATION;
            case ENUM_DECLARATION:
                return TTokenKind.ENUM_DECLARATION;
            case ANNOTATION_DECLARATION:
                return TTokenKind.ANNOTATION_DECLARATION;
            default:
                throw new AssertionError("Unexpected kind: " + kind);
        }
    }

    private static TTokenSpan convertTokenSpan(Token.Span span) {
        Preconditions.checkNotNull(span);
        TTokenSpan result = new TTokenSpan();
        result.setFrom(span.getFrom());
        result.setTo(span.getTo());
        return result;
    }
}
