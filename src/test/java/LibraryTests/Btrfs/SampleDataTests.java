//

package LibraryTests.Btrfs;

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

import DiscUtils.Btrfs.BtrfsFileSystem;
import DiscUtils.Btrfs.BtrfsFileSystemOptions;
import DiscUtils.Btrfs.Subvolume;
import DiscUtils.Core.DiscFileInfo;
import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FileSystemInfo;
import DiscUtils.Core.FileSystemManager;
import DiscUtils.Core.IFileSystem;
import DiscUtils.Core.LogicalVolumeInfo;
import DiscUtils.Core.VolumeManager;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Vhdx.Disk;
import DiscUtils.Vhdx.DiskImageFile;
import LibraryTests.Utilities.ZipUtilities;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.io.StreamReader;


public class SampleDataTests {
    @Test
    public void btrfsVhdxZip() throws Exception {
//        DiscUtils.Setup.SetupHelper.RegisterAssembly(Disk.class.getTypeInfo().Assembly);
//        DiscUtils.Setup.SetupHelper.RegisterAssembly(BtrfsFileSystem.class.getTypeInfo().Assembly);
        File fs = new File(URI.create(getClass().getResource("btrfs.zip").toString()));
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
            assertEquals("btrfs", filesystem.getName());
            try (DiscFileSystem btrfs = filesystem.open(volume)) {
                assertTrue(BtrfsFileSystem.class.isInstance(btrfs));
                assertEquals(1072594944, btrfs.getAvailableSpace());
                assertEquals(1072693248, btrfs.getSize());
                assertEquals(98304, btrfs.getUsedSpace());
                Subvolume[] subvolumes = ((BtrfsFileSystem) btrfs).getSubvolumes();
                assertEquals(1, subvolumes.length);
                assertEquals(256L, subvolumes[0].getId());
                assertEquals("subvolume", subvolumes[0].getName());
                assertEquals("text\n", getFileContent("\\folder\\subfolder\\file", btrfs));
                assertEquals("f64464c2024778f347277de6fa26fe87",
                             getFileChecksum("\\folder\\subfolder\\f64464c2024778f347277de6fa26fe87", btrfs));
                assertEquals("fa121c8b73cf3b01a4840b1041b35e9f",
                             getFileChecksum("\\folder\\subfolder\\fa121c8b73cf3b01a4840b1041b35e9f", btrfs));
                isAllZero("folder\\subfolder\\sparse", btrfs);
                assertEquals("test\n", getFileContent("\\subvolume\\subvolumefolder\\subvolumefile", btrfs));
                assertEquals("b0d5fae237588b6641f974459404d197", getFileChecksum("\\folder\\subfolder\\compressed", btrfs));
                assertEquals("test\n", getFileContent("\\folder\\symlink", btrfs));
                //PR#36
                assertEquals("b0d5fae237588b6641f974459404d197", getFileChecksum("\\folder\\subfolder\\lzo", btrfs));
            }
            try (BtrfsFileSystem subvolume = new BtrfsFileSystem(volume.open(), new BtrfsFileSystemOptions())) {
                assertEquals("test\n", getFileContent("\\subvolumefolder\\subvolumefile", subvolume));
            }
        }
    }

    @Test
    private static void isAllZero(String path, IFileSystem fs) throws Exception {
        DiscFileInfo fileInfo = fs.getFileInfo(path);
        byte[] buffer = new byte[4 * (int) Sizes.OneKiB];
        try (Stream file = fileInfo.openRead()) {
            int count = file.read(buffer, 0, buffer.length);
            for (int i = 0; i < count; i++) {
                assertEquals(0, buffer[i]);
            }
        }
    }

    @Test
    private static String getFileContent(String path, IFileSystem fs) throws Exception {
        DiscFileInfo fileInfo = fs.getFileInfo(path);

        try (StreamReader file = fileInfo.openText()) {
            return file.readToEnd();
        }
    }

    @Test
    private static String getFileChecksum(String path, IFileSystem fs) throws Exception {
        DiscFileInfo fileInfo = fs.getFileInfo(path);
        MessageDigest md5 = MessageDigest.getInstance("MD5");

        try (MemoryStream ms = new MemoryStream();
                Stream file = fileInfo.openRead()) {
            file.copyTo(ms);
            byte[] checksum = md5.digest(ms.toArray());
            return IntStream.range(0, checksum.length)
                    .mapToObj(i -> String.format("%02x", checksum[i]))
                    .collect(Collectors.joining());
        }
    }
}
