//

package LibraryTests.Utilities;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import moe.yo3explorer.dotnetio4j.MemoryStream;
import moe.yo3explorer.dotnetio4j.Stream;
import moe.yo3explorer.dotnetio4j.compat.JavaIOStream;


public class ZipUtilities {
    public static Stream readFileFromZip(File zip, String name) throws Exception {
        try (ZipFile zipArchive = new ZipFile(zip, ZipFile.OPEN_READ)) { // ZipArchiveMode.Read, true
            ZipEntry entry;
            if (name == null)
                entry = zipArchive.entries().nextElement();
            else
                entry = zipArchive.getEntry(name);
            MemoryStream ms = new MemoryStream();
            Stream zipFile = new JavaIOStream(zipArchive.getInputStream(entry));
            try {
                zipFile.copyTo(ms);
            } finally {
                if (zipFile != null)
                    zipFile.close();

            }
            return ms;
        }
    }
}
