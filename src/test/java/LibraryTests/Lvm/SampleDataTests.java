//

package LibraryTests.Lvm;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import DiscUtils.Core.LogicalVolumeInfo;
import DiscUtils.Core.VolumeManager;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Vhdx.Disk;
import DiscUtils.Vhdx.DiskImageFile;
import LibraryTests.Utilities.ZipUtilities;
import moe.yo3explorer.dotnetio4j.Stream;


public class SampleDataTests {

    @Test
    public void lvm2VhdxZip() throws Exception {
//        SetupHelper.setupComplete();
        File fs = new File(URI.create(getClass().getResource("lvm2.zip").toString()));
        try (Stream vhdx = ZipUtilities.readFileFromZip(fs, null);
                DiskImageFile diskImage = new DiskImageFile(vhdx, Ownership.Dispose);
                Disk disk = new Disk(Arrays.asList(diskImage), Ownership.Dispose)) {
            VolumeManager manager = new VolumeManager(disk);
            List<LogicalVolumeInfo> logicalVolumes = manager.getLogicalVolumes();
            assertEquals(3, logicalVolumes.size());
            assertEquals(1283457024, logicalVolumes.get(0).getLength());
            assertEquals(746586112, logicalVolumes.get(1).getLength());
            assertEquals(1178599424, logicalVolumes.get(2).getLength());
        }
    }
}
