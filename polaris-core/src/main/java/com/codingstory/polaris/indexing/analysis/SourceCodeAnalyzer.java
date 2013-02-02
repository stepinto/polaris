package com.codingstory.polaris.indexing.analysis;

import com.codingstory.polaris.sourcedb.SourceDbIndexedField;
import com.google.common.base.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.*;
import org.apache.lucene.util.Version;

import java.io.Reader;

/**
 * Created with IntelliJ IDEA.
 * User: yangshuguo
 * Date: 13-2-2
 * Time: 上午2:23
 * To change this template use File | Settings | File Templates.
 */
public final class SourceCodeAnalyzer extends Analyzer {

    Log LOG = LogFactory.getLog(SourceCodeAnalyzer.class);

    private final Version version;
    public SourceCodeAnalyzer(Version version) {
      this.version = version;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      if (Objects.equal(fieldName, SourceDbIndexedField.SOURCE_TERM)) {
        return new LowerCaseTokenizer(version, reader);
      } else {
        return new KeywordTokenizer(reader);
      }
    }
}
