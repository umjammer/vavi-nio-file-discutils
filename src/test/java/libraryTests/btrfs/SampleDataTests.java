//

package libraryTests.btrfs;

import java.io.File;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import discUtils.btrfs.BtrfsFileSystem;
import discUtils.btrfs.BtrfsFileSystemOptions;
import discUtils.btrfs.Subvolume;
import discUtils.core.DiscFileInfo;
import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemInfo;
import discUtils.core.FileSystemManager;
import discUtils.core.IFileSystem;
import discUtils.core.LogicalVolumeInfo;
import discUtils.core.VolumeManager;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import discUtils.vhdx.Disk;
import discUtils.vhdx.DiskImageFile;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.io.StreamReader;
import libraryTests.utilities.ZipUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SampleDataTests {

    private static final String FS = File.separator;

    @Test
    public void btrfsVhdxZip() throws Exception {
//        DiscUtils.setup.SetupHelper.RegisterAssembly(Disk.class.getTypeInfo().Assembly);
//        DiscUtils.setup.SetupHelper.RegisterAssembly(BtrfsFileSystem.class.getTypeInfo().Assembly);
        File fs = new File(URI.create(getClass().getResource("btrfs.zip").toString()));
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
            assertEquals("btrfs", filesystem.getName());
            try (DiscFileSystem btrfs = filesystem.open(volume)) {
                assertTrue(btrfs instanceof BtrfsFileSystem);
                assertEquals(1072594944, btrfs.getAvailableSpace());
                assertEquals(1072693248, btrfs.getSize());
                assertEquals(98304, btrfs.getUsedSpace());
                Subvolume[] subvolumes = ((BtrfsFileSystem) btrfs).getSubvolumes();
                assertEquals(1, subvolumes.length);
                assertEquals(256L, subvolumes[0].getId());
                assertEquals("subvolume", subvolumes[0].getName());
                assertEquals("text\n", getFileContent(FS + "folder" + FS + "subfolder" + FS + "file", btrfs));
                assertEquals("f64464c2024778f347277de6fa26fe87",
                             getFileChecksum(FS + "folder" + FS + "subfolder" + FS + "f64464c2024778f347277de6fa26fe87", btrfs));
                assertEquals("fa121c8b73cf3b01a4840b1041b35e9f",
                             getFileChecksum(FS + "folder" + FS + "subfolder" + FS + "fa121c8b73cf3b01a4840b1041b35e9f", btrfs));
                isAllZero("folder" + FS + "subfolder" + FS + "sparse", btrfs);
                assertEquals("test\n", getFileContent(FS + "subvolume" + FS + "subvolumefolder" + FS + "subvolumefile", btrfs));
                assertEquals("b0d5fae237588b6641f974459404d197", getFileChecksum(FS + "folder" + FS + "subfolder" + FS + "compressed", btrfs));
                assertEquals("test\n", getFileContent(FS + "folder" + FS + "symlink", btrfs)); //PR#36
                assertEquals("b0d5fae237588b6641f974459404d197", getFileChecksum(FS + "folder" + FS + "subfolder" + FS + "lzo", btrfs));
            }
            BtrfsFileSystemOptions options = new BtrfsFileSystemOptions();
            options.setSubvolumeId(256);
            options.setVerifyChecksums(true);
            try (BtrfsFileSystem subvolume = new BtrfsFileSystem(volume.open(), options)) {
                assertEquals("test\n", getFileContent(FS + "subvolumefolder" + FS + "subvolumefile", subvolume));
            }
        }
    }

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

    private static String getFileContent(String path, IFileSystem fs) throws Exception {
        DiscFileInfo fileInfo = fs.getFileInfo(path);

        try (StreamReader file = fileInfo.openText()) {
            return file.readToEnd();
        }
    }

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
