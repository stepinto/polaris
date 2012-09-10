package com.codingstory.polaris.indexing;

import com.codingstory.polaris.parser.*;
import com.google.common.base.Preconditions;

/**
 * Converters from POJOs to Thrift objects.
 */
public final class PojoToThriftConverter {

    private PojoToThriftConverter() {}

    public static TToken convertTypeDeclaration(TypeDeclaration typeDeclaration) {
        Preconditions.checkNotNull(typeDeclaration);
        TToken result = convertTokenCommon(typeDeclaration);
        result.setTypeDeclaration(doConvertTypeDeclaration(typeDeclaration));
        return result;
    }

    private static TTypeDeclaration doConvertTypeDeclaration(TypeDeclaration typeDeclaration) {
        TTypeDeclaration result = new TTypeDeclaration();
        result.setName(typeDeclaration.getName().toString());
        return result;
    }

    public static TToken convertFieldDeclaration(FieldDeclaration fieldDeclaration) {
        Preconditions.checkNotNull(fieldDeclaration);
        TToken result = convertTokenCommon(fieldDeclaration);
        result.setFieldDeclaration(doConvertFieldDeclaration(fieldDeclaration));
        return result;
    }

    private static TFieldDeclaration doConvertFieldDeclaration(FieldDeclaration fieldDeclaration) {
        TFieldDeclaration result = new TFieldDeclaration();
        // TODO: Convert package and class names
        result.setName(fieldDeclaration.getVariableName());
        result.setTypeReference(convertTypeReference(fieldDeclaration.getTypeReferenece()));
        return result;
    }

    public static TToken convertTypeUsage(TypeUsage typeUsage) {
        Preconditions.checkNotNull(typeUsage);
        TToken result = convertTokenCommon(typeUsage);
        result.setTypeUsage(doConvertTypeUsage(typeUsage));
        return result;
    }

    private static TTypeUsage doConvertTypeUsage(TypeUsage typeUsage) {
        TTypeUsage result = new TTypeUsage();
        result.setTypeReference(convertTypeReference(typeUsage.getTypeReference()));
        return result;
    }

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
            case FIELD_DECLARATION :
                return TTokenKind.FIELD_DECLARATION;
            case TYPE_USAGE:
                return TTokenKind.TYPE_USAGE;
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

    private static TTypeReference convertTypeReference(TypeReference typeReferenece) {
        Preconditions.checkNotNull(typeReferenece);
        TTypeReference result = new TTypeReference();
        result.setResolved(typeReferenece.isResoleved());
        if (typeReferenece.isResoleved()) {
            ResolvedTypeReference resolved = (ResolvedTypeReference) typeReferenece;
            result.addToCandidates(resolved.getName().toString());
        } else {
            UnresolvedTypeReferenece unresolved = (UnresolvedTypeReferenece) typeReferenece;
            for (FullyQualifiedTypeName name : unresolved.getCandidates()) {
                result.addToCandidates(name.toString());
            }
        }
        return result;
    }
}
