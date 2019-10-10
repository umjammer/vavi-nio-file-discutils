//

package LibraryTests.Helpers;

import DiscUtils.Core.CoreCompat.ReflectionHelper;
import moe.yo3explorer.dotnetio4j.DeflateStream.CompressionMode;
import moe.yo3explorer.dotnetio4j.MemoryStream;
import moe.yo3explorer.dotnetio4j.SeekOrigin;
import moe.yo3explorer.dotnetio4j.Stream;


public class Helpers {
    public static byte[] readAll(Stream stream) throws Exception {
        try (MemoryStream ms = new MemoryStream()) {
            stream.copyTo(ms);
            return ms.toArray();
        }
    }

    public static Stream loadDataFile(String name) throws Exception {
        Class<?> assembly = ReflectionHelper.getAssembly(Helpers.class);
        if (assembly.getManifestResourceNames().Contains(formattedName))
            return assembly.getManifestResourceStream(formattedName);

        // Try GZ
        formattedName += ".gz";
        if (assembly.getManifestResourceNames().Contains(formattedName)) {
            MemoryStream ms = new MemoryStream();
            try (Stream stream = assembly.getManifestResourceStream(formattedName)) {
                try (GZipStream gz = new GZipStream(stream, CompressionMode.Decompress)) {
                    gz.CopyTo(ms);
                }
            }
            ms.seek(0, SeekOrigin.Begin);
            return ms;
        }

        throw new Exception("Unable to locate embedded resource {name}");
    }
}
