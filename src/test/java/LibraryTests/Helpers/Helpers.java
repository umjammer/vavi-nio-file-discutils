//

package LibraryTests.Helpers;

import moe.yo3explorer.dotnetio4j.CompressionMode;
import moe.yo3explorer.dotnetio4j.GZipStream;
import moe.yo3explorer.dotnetio4j.MemoryStream;
import moe.yo3explorer.dotnetio4j.SeekOrigin;
import moe.yo3explorer.dotnetio4j.Stream;
import moe.yo3explorer.dotnetio4j.compat.JavaIOStream;


public class Helpers {
    public static byte[] readAll(Stream stream) throws Exception {
        try (MemoryStream ms = new MemoryStream()) {
            stream.copyTo(ms);
            return ms.toArray();
        }
    }

    public static Stream loadDataFile(Class<?> clazz, String name) throws Exception {
        // Try GZ
        MemoryStream ms = new MemoryStream();
        try (Stream stream = new JavaIOStream(clazz.getResourceAsStream(name), null);
                GZipStream gz = new GZipStream(stream, CompressionMode.Decompress)) {
            gz.copyTo(ms);
        }
        ms.seek(0, SeekOrigin.Begin);
        return ms;
    }
}
