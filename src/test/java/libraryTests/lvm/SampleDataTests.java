//

package libraryTests.lvm;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import discUtils.core.LogicalVolumeInfo;
import discUtils.core.VolumeManager;
import discUtils.streams.util.Ownership;
import discUtils.vhdx.Disk;
import discUtils.vhdx.DiskImageFile;
import dotnet4j.io.Stream;
import libraryTests.utilities.ZipUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class SampleDataTests {

    @Test
    public void lvm2VhdxZip() throws Exception {
//        SetupHelper.setupComplete();
        File fs = new File(URI.create(getClass().getResource("lvm2.zip").toString()));
        try (Stream vhdx = ZipUtilities.readFileFromZip(fs, null);
                DiskImageFile diskImage = new DiskImageFile(vhdx, Ownership.Dispose);
                Disk disk = new Disk(Collections.singletonList(diskImage), Ownership.Dispose)) {
            VolumeManager manager = new VolumeManager(disk);
            List<LogicalVolumeInfo> logicalVolumes = manager.getLogicalVolumes();
            assertEquals(3, logicalVolumes.size());

            assertEquals(1283457024, logicalVolumes.get(0).getLength());
            assertEquals(746586112, logicalVolumes.get(1).getLength());
            assertEquals(1178599424, logicalVolumes.get(2).getLength());
        }
    }
}
