//

package libraryTests.iso9660;

import java.io.File;
import java.net.URI;
import java.util.List;

import discUtils.core.DiscDirectoryInfo;
import discUtils.core.DiscFileInfo;
import discUtils.iso9660.CDReader;
import dotnet4j.io.Stream;
import libraryTests.utilities.ZipUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


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
