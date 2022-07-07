//

package libraryTests.helpers;

import dotnet4j.io.MemoryStream;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import dotnet4j.io.compat.JavaIOStream;
import dotnet4j.io.compression.CompressionMode;
import dotnet4j.io.compression.GZipStream;


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
