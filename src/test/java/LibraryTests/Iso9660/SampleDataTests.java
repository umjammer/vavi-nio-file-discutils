//

package LibraryTests.Iso9660;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import DiscUtils.Core.DiscDirectoryInfo;
import DiscUtils.Core.DiscFileInfo;
import DiscUtils.Iso9660.CDReader;
import LibraryTests.Utilities.ZipUtilities;
import dotnet4j.io.Stream;


public class SampleDataTests {
    @Test
    public void appleTestZip() throws Exception {
        File fs = new File(URI.create(getClass().getResource("apple-test.zip").toString()));
        try (Stream iso = ZipUtilities.readFileFromZip(fs, null);
                CDReader cr = new CDReader(iso, false)) {
            DiscDirectoryInfo dir = cr.getDirectoryInfo("sub-directory");
            assertNotNull(dir);
            assertEquals("sub-directory", dir.getName());
            List<DiscFileInfo> file = dir.getFiles("apple-test.txt"); // TODO
            assertEquals(1, file.size());
            assertEquals(21, file.get(0).getLength());
            assertEquals("apple-test.txt", file.get(0).getName());
            assertEquals(dir, file.get(0).getDirectory());
        }
    }
}
