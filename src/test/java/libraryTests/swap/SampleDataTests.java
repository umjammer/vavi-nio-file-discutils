//

package libraryTests.swap;

import java.io.File;
import java.net.URI;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import discUtils.complete.SetupHelper;
import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemInfo;
import discUtils.core.FileSystemManager;
import discUtils.core.LogicalVolumeInfo;
import discUtils.core.VolumeManager;
import discUtils.streams.util.Ownership;
import discUtils.swap.SwapFileSystem;
import discUtils.vhdx.Disk;
import discUtils.vhdx.DiskImageFile;
import libraryTests.utilities.ZipUtilities;
import dotnet4j.io.Stream;


public class SampleDataTests {
    @Test
    public void swapVhdxGzip() throws Exception {
        SetupHelper.setupComplete();
        File fs = new File(URI.create(getClass().getResource("swap.zip").toString()));
        try (Stream vhdx = ZipUtilities.readFileFromZip(fs, null);
                DiskImageFile diskImage = new DiskImageFile(vhdx, Ownership.Dispose);
                Disk disk = new Disk(Collections.singletonList(diskImage), Ownership.Dispose)) {

            VolumeManager manager = new VolumeManager(disk);
            List<LogicalVolumeInfo> logicalVolumes = manager.getLogicalVolumes();
            assertEquals(1, logicalVolumes.size());

            LogicalVolumeInfo volume = logicalVolumes.get(0);
            List<FileSystemInfo> filesystems = FileSystemManager.detectFileSystems(volume);
            assertEquals(1, filesystems.size());

            FileSystemInfo filesystem = filesystems.get(0);
            assertEquals("swap", filesystem.getName());

            DiscFileSystem swap = filesystem.open(volume);
            assertTrue(swap instanceof SwapFileSystem);

            assertEquals(0, swap.getAvailableSpace());
            assertEquals(10737414144L, swap.getSize());
            assertEquals(swap.getSize(), swap.getUsedSpace());
        }
    }
}
