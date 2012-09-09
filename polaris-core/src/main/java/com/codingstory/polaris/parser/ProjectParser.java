package com.codingstory.polaris.parser;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Scans source files under a base directory, and resolves type/method references.
 */
public class ProjectParser {
    public static interface TokenCollector {
        void collect(File file, byte[] content, List<Token> tokens);
    }

    public static class Stats {
        public int successFiles;
        public int failedFiles;
        public int types;
        public int bytes;
        public int lines;
    }

    /** Resolves type references at project level. */
    private static class ProjectTypeResolver implements TypeResolver {
        private Map<FullyQualifiedTypeName, TypeDeclaration> types = Maps.newHashMap();

        public void register(TypeDeclaration typeDeclaration) {
            Preconditions.checkNotNull(typeDeclaration);
            types.put(typeDeclaration.getName(), typeDeclaration);
        }

        @Override
        public ResolvedTypeReference resolve(UnresolvedTypeReferenece unresolved) {
            Preconditions.checkNotNull(unresolved);
            for (FullyQualifiedTypeName name : unresolved.getCandidates()) {
                TypeDeclaration typeDeclaration = types.get(name);
                if (typeDeclaration != null) {
                    ResolvedTypeReference resolved = new ResolvedTypeReference(name);
                    LOGGER.debug(String.format("Resolved %s as %s",
                            unresolved.getUnqualifiedName(),
                            resolved.getName()));
                    return resolved;
                }
            }
            return null;
        }
    }

    private static final TokenCollector NO_OP_TOKEN_EXTRACTOR = new TokenCollector() {
        @Override
        public void collect(File file, byte[] content, List<Token> tokens) {
            // No-op
        }
    };
    private static final Log LOGGER = LogFactory.getLog(ProjectParser.class);

    private ParserOptions parserOptions = new ParserOptions();
    private TokenCollector tokenCollector = NO_OP_TOKEN_EXTRACTOR;
    private Stats stats;
    private List<File> sourceFiles = Lists.newArrayList();
    private ProjectTypeResolver typeResolver;

    public void setTokenCollector(TokenCollector tokenCollector) {
        this.tokenCollector = tokenCollector;
    }

    public void setParserOptions(ParserOptions parserOptions) {
        this.parserOptions = parserOptions;
    }

    public void addSourceFile(File file) {
        Preconditions.checkNotNull(file);
        sourceFiles.add(file);
    }

    public void run() throws IOException {
        Preconditions.checkNotNull(sourceFiles);
        prepare();
        for (File f : sourceFiles) {
            try {
                runFirstPass(f);
                stats.successFiles++;
            } catch (IOException e) {
                if (parserOptions.isFailFast()) {
                    throw e;
                } else {
                    stats.failedFiles++;
                }
            }
        }
        for (File f : sourceFiles) {
            try {
                runSecondPass(f);
                stats.successFiles++;
            } catch (IOException e) {
                if (parserOptions.isFailFast()) {
                    throw e;
                } else {
                    stats.failedFiles++;
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
        InputStream in = new FileInputStream(sourceFile);
        try {
            List<Token> tokens = TokenExtractor.extract(in, TypeResolver.NO_OP_RESOLVER);
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
        byte[] content = FileUtils.readFileToByteArray(sourceFile);
        List<Token> tokens = TokenExtractor.extract(
                new ByteArrayInputStream(content), typeResolver);
        LOGGER.debug("Found " + tokens.size() + " token(s)");
        tokenCollector.collect(sourceFile, content, tokens);
    }

    public Stats getStats() {
        return stats;
    }
}
