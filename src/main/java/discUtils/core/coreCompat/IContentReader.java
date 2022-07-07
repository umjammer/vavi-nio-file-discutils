
package discUtils.core.coreCompat;

import java.io.Closeable;
import java.util.List;

import dotnet4j.io.SeekOrigin;


public interface IContentReader extends Closeable {
    List<?> read(long readCount);

    void seek(long offset, SeekOrigin origin);

    void close();
}
