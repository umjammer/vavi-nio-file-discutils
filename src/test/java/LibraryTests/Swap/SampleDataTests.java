//

package LibraryTests.Swap;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Complete.SetupHelper;
import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FileSystemInfo;
import DiscUtils.Core.FileSystemManager;
import DiscUtils.Core.LogicalVolumeInfo;
import DiscUtils.Core.VolumeManager;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Swap.SwapFileSystem;
import DiscUtils.Vhdx.Disk;
import DiscUtils.Vhdx.DiskImageFile;
import LibraryTests.Utilities.ZipUtilities;
import dotnet4j.io.Stream;


public class SampleDataTests {
    @Test
    public void swapVhdxGzip() throws Exception {
        SetupHelper.setupComplete();
        File fs = new File(URI.create(getClass().getResource("swap.zip").toString()));
        try (Stream vhdx = ZipUtilities.readFileFromZip(fs, null);
                DiskImageFile diskImage = new DiskImageFile(vhdx, Ownership.Dispose);
                Disk disk = new Disk(Arrays.asList(diskImage), Ownership.Dispose)) {

            VolumeManager manager = new VolumeManager(disk);
            List<LogicalVolumeInfo> logicalVolumes = manager.getLogicalVolumes();
            assertEquals(1, logicalVolumes.size());

            LogicalVolumeInfo volume = logicalVolumes.get(0);
            List<FileSystemInfo> filesystems = FileSystemManager.detectFileSystems(volume);
            assertEquals(1, filesystems.size());

            FileSystemInfo filesystem = filesystems.get(0);
            assertEquals("Swap", filesystem.getName());

            DiscFileSystem swap = filesystem.open(volume);
            assertTrue(SwapFileSystem.class.isInstance(swap));

            assertEquals(0, swap.getAvailableSpace());
            assertEquals(10737414144L, swap.getSize());
            assertEquals(swap.getSize(), swap.getUsedSpace());
        }
    }
}
