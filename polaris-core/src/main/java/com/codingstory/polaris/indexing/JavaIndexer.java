package com.codingstory.polaris.indexing;

import com.codingstory.polaris.parser.*;
import com.google.common.base.Preconditions;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexWriter;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.xerial.snappy.Snappy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import static com.codingstory.polaris.indexing.FieldName.*;

/**
 * Created with IntelliJ IDEA.
 * User: Zhou Yunqing
 * Date: 12-8-17
 * Time: 下午7:21
 * To change this template use File | Settings | File Templates.
 */
public class JavaIndexer {

    private static Log LOG = LogFactory.getLog(JavaIndexer.class);
    private final IndexWriter writer;
    private final String projectName;

    public JavaIndexer(IndexWriter writer, String projectName) {
        this.writer = Preconditions.checkNotNull(writer);
        this.projectName = Preconditions.checkNotNull(projectName);
    }

    private void addIndexFieldToDocument(Document document, String fieldName, String content) {
        Fieldable f = new Field(
                fieldName,
                content,
                Field.Store.YES,
                Field.Index.ANALYZED,
                Field.TermVector.WITH_POSITIONS_OFFSETS);
        f.setBoost(2.0f);
        document.add(f);
    }

    /**
     * Indexes file content.
     */
    public void indexFile(String filePath, byte[] content, List<Token> tokens) throws IOException {
        Preconditions.checkNotNull(filePath);
        Preconditions.checkNotNull(content);
        Preconditions.checkNotNull(tokens);
        byte[] sha1sum = DigestUtils.sha(content);
        LOG.debug("Indexing file content: " + projectName + filePath);
        Document document = new Document();
        document.add(new Field(FILE_ID, Hex.encodeHexString(sha1sum), Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field(FILE_CONTENT, Snappy.compress(content)));
        document.add(new Field(PROJECT_NAME, projectName, Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field(FILE_NAME, filePath, Field.Store.YES, Field.Index.NOT_ANALYZED));
        // document.add(new Field(TOKENS, serializeTokens(tokens)));
        document.add(new Field(SOURCE_ANNOTATIONS,
                Snappy.compress(SourceAnnotator.annotate(new ByteArrayInputStream(content), tokens))));
        document.add(new Field(DIRECTORY_NAME, getParentPath(filePath), Field.Store.YES, Field.Index.NOT_ANALYZED));
        writer.addDocument(document);
        for (Token token : tokens) {
            indexToken(projectName, filePath, sha1sum, token);
        }
    }

    private byte[] serializeTokens(List<Token> tokens) {
        try {
            TSerializer serializer = new TSerializer(new TBinaryProtocol.Factory());
            TTokenList list = new TTokenList();
            for (Token token : tokens) {
                if (token instanceof TypeDeclaration) {
                    list.addToTokens(PojoToThriftConverter.convertTypeDeclaration((TypeDeclaration) token));
                } else if (token instanceof FieldDeclaration) {
                    list.addToTokens(PojoToThriftConverter.convertFieldDeclaration((FieldDeclaration) token));
                } else if (token instanceof TypeUsage) {
                    list.addToTokens(PojoToThriftConverter.convertTypeUsage((TypeUsage) token));
                }
                // TODO: Convert more types.
            }
            return serializer.serialize(list);
        } catch (TException e) {
            throw new AssertionError(e); // Should not reach here.
        }
    }

    private void indexToken(String projectName, String filePath, byte[] sha1sum, Token t) throws IOException {
        Preconditions.checkNotNull(projectName);
        Preconditions.checkNotNull(filePath);
        Preconditions.checkNotNull(t);
        Document document = new Document();
        document.add(new Field(PROJECT_NAME, projectName, Field.Store.YES, Field.Index.NO));
        document.add(new Field(FILE_NAME, filePath, Field.Store.YES, Field.Index.NO));
        document.add(new Field(FILE_ID, Hex.encodeHexString(sha1sum), Field.Store.YES, Field.Index.NO));
        String offset = Long.toString(t.getSpan().getFrom());
        document.add(new Field(OFFSET, offset, Field.Store.YES, Field.Index.NO));
        document.add(new Field(KIND, String.valueOf(t.getKind().ordinal()), Field.Store.YES, Field.Index.NOT_ANALYZED));
        if (EnumSet.of(Token.Kind.CLASS_DECLARATION,
                Token.Kind.INTERFACE_DECLARATION,
                Token.Kind.ENUM_DECLARATION,
                Token.Kind.ANNOTATION_DECLARATION).contains(t.getKind())) {
            TypeDeclaration declaration = (TypeDeclaration) t;
            processType(document, declaration);
        } else if (t.getKind() == Token.Kind.METHOD_DECLARATION) {
            MethodDeclaration declaration = (MethodDeclaration) t;
            processMethod(document, declaration);
        } else if (t.getKind() == Token.Kind.PACKAGE_DECLARATION) {
            PackageDeclaration declaration = (PackageDeclaration) t;
            addIndexFieldToDocument(document, PACKAGE_NAME, declaration.getPackageName());
        } else if (t.getKind() == Token.Kind.FIELD_DECLARATION) {
            FieldDeclaration declaration = (FieldDeclaration) t;
            processField(document, declaration);
        }
        writer.addDocument(document);
    }

    private void processField(Document document, FieldDeclaration declaration) {
        addIndexFieldToDocument(document, FIELD_NAME, declaration.getVariableName());
        addIndexFieldToDocument(document, FIELD_NAME, declaration.getName().toString());
        TypeReference type = declaration.getTypeReference();
        addIndexFieldToDocument(document, FIELD_TYPE_NAME, type.getUnqualifiedName());
        if (type.isResoleved()) {
            ResolvedTypeReference resolved = (ResolvedTypeReference) type;
            addIndexFieldToDocument(document, FIELD_TYPE_NAME, resolved.getName().toString());
        } else {
            // TODO: Bug? The following lines result in bad ranking.
//            UnresolvedTypeReferenece unresolved = (UnresolvedTypeReferenece) type;
//            for (FullyQualifiedTypeName candidate : unresolved.getCandidates()) {
//                addIndexFieldToDocument(document, FIELD_TYPE_NAME, candidate.toString());
//            }
        }
    }

    private void processMethod(Document document, MethodDeclaration declaration) {
        String name = declaration.getMethodName();
        addIndexFieldToDocument(document, METHOD_NAME, name);
        String fullName = declaration.getPackageName() + "." + declaration.getClassName() + "." + name;
        addIndexFieldToDocument(document, METHOD_NAME, fullName);
    }

    private void processType(Document document, TypeDeclaration declaration) {
        FullyQualifiedTypeName name = declaration.getName();
        addIndexFieldToDocument(document, TYPE_NAME, name.getTypeName());
        String fullName = name.getTypeName();
        if (name.hasPackageName())
            fullName = name.getPackageName() + "." + fullName;
        addIndexFieldToDocument(document, TYPE_NAME, fullName);
        if (declaration.hasJavaDocComment()) {
            addIndexFieldToDocument(document, JAVA_DOC, declaration.getJavaDocComment());
        }
    }

    private static String getParentPath(String path) {
        Preconditions.checkNotNull(path);
        int lastSlash = path.lastIndexOf(File.separatorChar);
        Preconditions.checkArgument(lastSlash != -1, "Bad path: " + path);
        return path.substring(0, lastSlash);
    }
}
