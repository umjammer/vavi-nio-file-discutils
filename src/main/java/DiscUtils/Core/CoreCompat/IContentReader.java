
package DiscUtils.Core.CoreCompat;

import java.io.Closeable;
import java.util.List;

import moe.yo3explorer.dotnetio4j.SeekOrigin;


public interface IContentReader extends Closeable {
    List<?> read(long readCount);

    void seek(long offset, SeekOrigin origin);

    void close();
}
