//

package LibraryTests.Utilities;

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
//System.err.println(entry);
//System.err.println(StringUtil.getDump(zipArchive.getInputStream(entry), 0x300000, 128));
            MemoryStream ms = new MemoryStream();
            try (Stream zipFile = new JavaIOStream(zipArchive.getInputStream(entry))) {
                zipFile.copyTo(ms);
            }
//long c1 = Checksum.getChecksum(zipArchive.getInputStream(entry));
//long c2 = Checksum.getChecksum(new ByteArrayInputStream(ms.toArray()));
//assertEquals(c1, c2);
//System.err.println(StringUtil.getDump(ms.toArray(), 512));
            return ms;
        }
    }
}
