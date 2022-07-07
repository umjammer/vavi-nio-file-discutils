//

package libraryTests.utilities;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.io.compat.JavaIOStream;


public class ZipUtilities {

    public static Stream readFileFromZip(File zip, String name) throws Exception {
        try (ZipFile zipArchive = new ZipFile(zip, ZipFile.OPEN_READ)) { // ZipArchiveMode.Read, true
            ZipEntry entry;
            if (name == null)
                entry = zipArchive.entries().nextElement();
            else
                entry = zipArchive.getEntry(name);
            MemoryStream ms = new MemoryStream();
            try (Stream zipFile = new JavaIOStream(zipArchive.getInputStream(entry))) {
                zipFile.copyTo(ms);
            }
//MemoryStream.debug = true;
            return ms;
        }
    }
}
