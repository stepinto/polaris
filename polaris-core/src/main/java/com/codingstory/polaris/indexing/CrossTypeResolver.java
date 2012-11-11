package com.codingstory.polaris.indexing;

import com.codingstory.polaris.SkipCheckingExceptionWrapper;
import com.codingstory.polaris.parser.ClassType;
import com.codingstory.polaris.parser.FullTypeName;
import com.codingstory.polaris.parser.TypeHandle;
import com.codingstory.polaris.parser.TypeResolver;
import com.codingstory.polaris.typedb.TypeDb;
import com.google.common.base.Preconditions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.List;

public class CrossTypeResolver implements TypeResolver {

    private static final Log LOG = LogFactory.getLog(CrossTypeResolver.class);
    private final TypeDb typeDb;

    public CrossTypeResolver(TypeDb typeDb) {
        this.typeDb = Preconditions.checkNotNull(typeDb);
    }

    @Override
    public TypeHandle resolve(FullTypeName name) {
        Preconditions.checkNotNull(name);
        try {
            List<ClassType> result = typeDb.queryByTypeName(name);
            if (result.isEmpty()) {
                return null;
            }
            if (result.size() > 1) {
                LOG.debug("Ambiguous type: " + name);
            }
            return result.get(0).getHandle();
        } catch (IOException e) {
            throw new SkipCheckingExceptionWrapper(e);
        }
    }
}
