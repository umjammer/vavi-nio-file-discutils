
package discUtils.core.coreCompat;

import java.io.Closeable;
import java.util.List;

import dotnet4j.io.SeekOrigin;


public interface IContentWriter extends Closeable {

    List<?> write(List<?> content);

    void seek(long offset, SeekOrigin origin);
}
