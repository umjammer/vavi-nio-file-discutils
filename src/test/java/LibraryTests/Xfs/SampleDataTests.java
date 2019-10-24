//

package LibraryTests.Xfs;

import java.io.File;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import DiscUtils.Vhdx.Disk;
import DiscUtils.Vhdx.DiskImageFile;
import DiscUtils.Xfs.XfsFileSystem;
import LibraryTests.Utilities.ZipUtilities;
import dotnet4j.io.FileMode;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;


public class SampleDataTests {
    @Test
    public void xfsVhdxZip() throws Exception {
        SetupHelper.setupComplete();
        File fs = new File(URI.create(getClass().getResource("xfs.zip").toString()));
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
            assertEquals("xfs", filesystem.getName());
            try (DiscFileSystem xfs = filesystem.open(volume)) {
                assertTrue(XfsFileSystem.class.isInstance(xfs));
                assertEquals(9081139200L, xfs.getAvailableSpace());
                assertEquals(10725863424L, xfs.getSize());
                assertEquals(1644724224, xfs.getUsedSpace());
                validateContent(xfs);
            }
        }
    }

    @Test
    public void xfs5VhdxZip() throws Exception {
        SetupHelper.setupComplete();
        File fs = new File(URI.create(getClass().getResource("xfs5.zip").toString()));
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
            assertEquals("xfs", filesystem.getName());
            try (DiscFileSystem xfs = filesystem.open(volume)) {
                assertTrue(XfsFileSystem.class.isInstance(xfs));
                assertEquals(9080827904L, xfs.getAvailableSpace());
                assertEquals(10725883904L, xfs.getSize());
                assertEquals(1645056000, xfs.getUsedSpace());
                validateContent(xfs);
            }
        }
    }

    @Test
    private void validateContent(DiscFileSystem xfs) throws Exception {
        assertTrue(xfs.directoryExists(""));
        assertTrue(xfs.fileExists("folder\\nested\\file"));
        assertEquals(0, xfs.getFileSystemEntries("empty").size());
        for (int i = 1; i <= 1000; i++) {
            assertTrue(xfs.fileExists("folder\\file.{i}"), "File file.{i} not found");
        }

        try (Stream file = xfs.openFile("folder\\file.100", FileMode.Open); MemoryStream ms = new MemoryStream()) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            file.copyTo(ms);
            byte[] checksum = md5.digest(ms.toArray());
            assertEquals("620f0b67a91f7f74151bc5be745b7110",
                         IntStream.range(0, checksum.length)
                                 .mapToObj(i -> String.format("%02x", checksum[i]))
                                 .collect(Collectors.joining()));
        }

        try (Stream file = xfs.openFile("folder\\file.random", FileMode.Open); MemoryStream ms = new MemoryStream()) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            file.copyTo(ms);
            byte[] checksum = md5.digest(ms.toArray());
            assertEquals("9a202a11d6e87688591eb97714ed56f1",
                         IntStream.range(0, checksum.length)
                                 .mapToObj(i -> String.format("%02x", checksum[i]))
                                 .collect(Collectors.joining()));
        }
        for (int i = 1; i <= 999; i++) {
            assertTrue(xfs.fileExists("huge\\{i}"), "File huge/{i} not found");
        }
    }
}
