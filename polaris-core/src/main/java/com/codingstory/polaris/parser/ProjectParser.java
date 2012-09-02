package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Scans source files under a base directory, and resolves type/method references.
 */
public class ProjectParser {
    public static interface TokenCollector {
        void collect(File file, Token token);
    }

    public static class Stats {
        public int successFiles;
        public int failedFiles;
        public int types;
        public int bytes;
        public int lines;
    }

    private static class ProjectTypeResolver implements TypeResolver {
        private Map<FullyQualifiedName, TypeDeclaration> types = Maps.newHashMap();

        public void register(TypeDeclaration typeDeclaration) {
            Preconditions.checkNotNull(typeDeclaration);
            types.put(typeDeclaration.getName(), typeDeclaration);
        }

        @Override
        public ResolvedTypeReference resolve(UnresolvedTypeReferenece typeReferenece) {
            for (FullyQualifiedName name : typeReferenece.getCandidates()) {
                TypeDeclaration typeDeclaration = types.get(name);
                if (typeDeclaration != null) {
                    return new ResolvedTypeReference(name);
                }
            }
            return null;
        }
    }

    private static final TokenCollector NO_OP_TOKEN_EXTRACTOR = new TokenCollector() {
        @Override
        public void collect(File file, Token token) {
            // No-op
        }
    };
    private static final Log LOGGER = LogFactory.getLog(ProjectParser.class);

    private boolean ignoreErrors = false;
    private TokenCollector tokenCollector = NO_OP_TOKEN_EXTRACTOR;
    private String projectName;
    private Stats stats;
    private List<File> sourceFiles = Lists.newArrayList();
    private ProjectTypeResolver typeResolver;

    public void setProjectName(@NotNull String projectName) {
        Preconditions.checkNotNull(projectName);
        this.projectName = projectName;
    }

    public void setTokenCollector(TokenCollector tokenCollector) {
        this.tokenCollector = tokenCollector;
    }

    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    public void addSourceFile(File file) {
        Preconditions.checkNotNull(file);
        sourceFiles.add(file);
    }

    public void run() throws IOException {
        Preconditions.checkNotNull(projectName);
        Preconditions.checkNotNull(sourceFiles);
        prepare();
        for (File f : sourceFiles) {
            try {
                runFirstPass(f);
                stats.successFiles++;
            } catch (IOException e) {
                if (ignoreErrors) {
                    stats.failedFiles++;
                } else {
                    throw e;
                }
            }
        }
        for (File f : sourceFiles) {
            try {
                runSecondPass(f);
                stats.successFiles++;
            } catch (IOException e) {
                if (ignoreErrors) {
                    stats.failedFiles++;
                } else {
                    throw e;
                }
            }
        }
        stats.successFiles /= 2;
        stats.failedFiles /= 2;
    }

    private void prepare() {
        stats = new Stats();
        typeResolver = new ProjectTypeResolver();
    }

    private void runFirstPass(File sourceFile) throws IOException {
        LOGGER.debug("Parsing " + sourceFile + " (1st pass)");
        TokenExtractor extractor = new TokenExtractor();
        InputStream in = new FileInputStream(sourceFile);
        try {
            extractor.setInputStream(in);
            List<Token> tokens = extractor.extractTokens();
            for (Token token : tokens) {
                if (token instanceof TypeDeclaration) {
                    TypeDeclaration typeDeclaration = (TypeDeclaration) token;
                    typeResolver.register(typeDeclaration);
                    LOGGER.debug("Found type: " + typeDeclaration.getName());
                }
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void runSecondPass(File sourceFile) throws IOException {
        LOGGER.debug("Parsing " + sourceFile + " (2nd pass)");
        TokenExtractor extractor = new TokenExtractor();
        InputStream in = new FileInputStream(sourceFile);
        try {
            extractor.setInputStream(in);
            List<Token> tokens = extractor.extractTokens();
            for (Token token : tokens) {
                Token result = token;
                if (token instanceof TypeDeclaration) {
                    stats.types++;
                } else if (token instanceof FieldDeclaration) {
                    FieldDeclaration fieldDeclaration = (FieldDeclaration) token;
                    TypeReference typeReference = fieldDeclaration.getTypeReferenece();
                    if (!typeReference.isResoleved()) {
                        UnresolvedTypeReferenece unresolved = (UnresolvedTypeReferenece) typeReference;
                        ResolvedTypeReference resolved = typeResolver.resolve(unresolved);
                        if (resolved != null) {
                            result = FieldDeclaration.newBuilder()
                                    .setClassName(fieldDeclaration.getClassName())
                                    .setPackageName(fieldDeclaration.getPackageName())
                                    .setSpan(fieldDeclaration.getSpan())
                                    .setTypeReference(resolved)
                                    .setVariableName(fieldDeclaration.getPackageName())
                                    .build();
                            LOGGER.debug(String.format("Resolved %s as %s at field %s.%s.%s",
                                    unresolved.getUnqualifiedName(),
                                    resolved.getName(),
                                    fieldDeclaration.getPackageName(),
                                    fieldDeclaration.getClassName(),
                                    fieldDeclaration.getVariableName()));
                        }
                    }
                    // TODO: Resolve method signature.
                }
                tokenCollector.collect(sourceFile, result);
            }
            LOGGER.debug("Found " + tokens.size() + " token(s)");
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public Stats getStats() {
        return stats;
    }
}
