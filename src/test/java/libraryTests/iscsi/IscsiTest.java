/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.iscsi;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import discUtils.core.partitions.BiosPartitionTable;
import discUtils.core.partitions.WellKnownPartitionType;
import discUtils.fat.FatFileSystem;
import discUtils.iscsi.Disk;
import discUtils.iscsi.Initiator;
import discUtils.iscsi.LunClass;
import discUtils.iscsi.LunInfo;
import discUtils.iscsi.Session;
import discUtils.iscsi.TargetInfo;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.junit.jupiter.api.io.TempDir;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static libraryTests.iscsi.IscsiTargetServer.BLOCK_SIZE;
import static libraryTests.iscsi.IscsiTargetServer.TARGET_NAME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * {@link discUtils.iscsi} initiator against a jSCSI target, i.e. the client and
 * the server side of iSCSI in one VM.
 * <p>
 * Every test starts its own target, because jSCSI drops a target from its
 * portal and closes the backing storage as soon as a normal session ends: one
 * target serves one session and no more.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @see IscsiTargetServer
 */
@Timeout(value = 30, threadMode = ThreadMode.SEPARATE_THREAD)
class IscsiTest {

    private static final String FS = File.separator;

    /** the lun serving {@link #PATTERN}, only ever written to outside of block 0 */
    static final int DISK_SIZE = 4 * 1024 * 1024;

    /** the lun the file system test formats, fat wants more than 8400 sectors */
    static final int FORMATTED_SIZE = 32 * 1024 * 1024;

    /** written to block 0 of {@link #image} before any target is started */
    static final byte[] PATTERN = new byte[BLOCK_SIZE];

    static Path image;

    static Path formatted;

    @BeforeAll
    static void beforeAll(@TempDir Path dir) throws Exception {
        for (int i = 0; i < PATTERN.length; i++) {
            PATTERN[i] = (byte) (i * 7 + 1);
        }

        image = dir.resolve("lun0.img");
        try (RandomAccessFile file = new RandomAccessFile(image.toFile(), "rw")) {
            file.setLength(DISK_SIZE);
            file.write(PATTERN);
        }
        assertEquals(DISK_SIZE, Files.size(image));

        formatted = dir.resolve("lun1.img");
        try (RandomAccessFile file = new RandomAccessFile(formatted.toFile(), "rw")) {
            file.setLength(FORMATTED_SIZE);
        }
    }

    @Test
    @DisplayName("discovery: SendTargets lists the target of the portal")
    void discovery() throws Exception {
        try (IscsiTargetServer target = new IscsiTargetServer(image)) {
            Initiator initiator = new Initiator();
            TargetInfo[] targets = initiator.getTargets(target.getPortalAddress());

            assertEquals(1, targets.length, "targets: " + Arrays.toString(targets));
            assertEquals(TARGET_NAME, targets[0].getName());
            assertFalse(targets[0].getAddresses().isEmpty());
        }
    }

    @Test
    @DisplayName("a normal session reports the served lun")
    void luns() throws Exception {
        try (IscsiTargetServer target = new IscsiTargetServer(image);
                Session session = new Initiator().connectTo(TARGET_NAME, target.getPortalAddress())) {
            LunInfo[] luns = session.getLuns();
            assertEquals(1, luns.length, "luns: " + Arrays.toString(luns));
            assertEquals(0, luns[0].getLun());
            assertEquals(LunClass.BlockStorage, luns[0].getDeviceType());
            assertFalse(luns[0].getRemovable());

            assertEquals(List.of(0L), session.getBlockDeviceLuns());
        }
    }

    @Test
    @DisplayName("read capacity matches the file the target serves")
    void capacity() throws Exception {
        try (IscsiTargetServer target = new IscsiTargetServer(image);
                Session session = new Initiator().connectTo(TARGET_NAME, target.getPortalAddress())) {
            assertEquals(BLOCK_SIZE, session.getCapacity(0).getBlockSize());
            assertEquals(DISK_SIZE / BLOCK_SIZE, session.getCapacity(0).getLogicalBlockCount());

            Disk disk = new Disk(session, 0, FileAccess.Read);
            assertEquals(BLOCK_SIZE, disk.getBlockSize());
            assertEquals(DISK_SIZE, disk.getCapacity());
        }
    }

    @Test
    @DisplayName("read blocks the target was given before it started")
    void read() throws Exception {
        try (IscsiTargetServer target = new IscsiTargetServer(image);
                Session session = new Initiator().connectTo(TARGET_NAME, target.getPortalAddress())) {
            byte[] buffer = new byte[BLOCK_SIZE];
            session.read(0, 0, (short) 1, buffer, 0);
            assertArrayEquals(PATTERN, buffer);

            // the same, through the virtual disk abstraction
            Stream content = new Disk(session, 0, FileAccess.Read).getContent();
            byte[] read = new byte[BLOCK_SIZE];
            content.position(0);
            assertEquals(BLOCK_SIZE, content.read(read, 0, read.length));
            assertArrayEquals(PATTERN, read);
            assertEquals(DISK_SIZE, content.getLength());
        }
    }

    @Test
    @DisplayName("write a block and read it back, on the target and on the file")
    void write() throws Exception {
        long block = 100;
        byte[] written = new byte[BLOCK_SIZE];
        Arrays.fill(written, (byte) 0x5a);

        try (IscsiTargetServer target = new IscsiTargetServer(image);
                Session session = new Initiator().connectTo(TARGET_NAME, target.getPortalAddress())) {
            session.write(0, block, (short) 1, BLOCK_SIZE, written, 0);

            byte[] buffer = new byte[BLOCK_SIZE];
            session.read(0, block, (short) 1, buffer, 0);
            assertArrayEquals(written, buffer);
        }

        // and it really landed in the file the target serves
        byte[] onDisk = new byte[BLOCK_SIZE];
        try (RandomAccessFile file = new RandomAccessFile(image.toFile(), "r")) {
            file.seek(block * BLOCK_SIZE);
            file.readFully(onDisk);
        }
        assertArrayEquals(written, onDisk);
    }

    @Test
    @DisplayName("partition and format a lun, then read the file system back")
    void fileSystemOnLun() throws Exception {
        byte[] content = "over iscsi".getBytes(US_ASCII);

        try (IscsiTargetServer target = new IscsiTargetServer(formatted);
                Session session = new Initiator().connectTo(TARGET_NAME, target.getPortalAddress())) {
            Disk disk = new Disk(session, 0, FileAccess.ReadWrite);

            BiosPartitionTable table = BiosPartitionTable.initialize(disk, WellKnownPartitionType.WindowsFat);
            assertEquals(1, table.getPartitions().size());

            try (FatFileSystem fs = FatFileSystem.formatPartition(disk, 0, "ISCSI      ")) {
                fs.createDirectory("DIR");
                try (Stream file = fs.openFile("DIR" + FS + "HELLO.TXT", FileMode.Create)) {
                    file.write(content, 0, content.length);
                }
            }

            // everything above went over the wire, so read it back the same way
            try (Stream partition = disk.getPartitions().get(0).open();
                    FatFileSystem fs = new FatFileSystem(partition)) {
                assertTrue(fs.directoryExists("DIR"), "directories: " + fs.getDirectories(""));
                assertTrue(fs.fileExists("DIR" + FS + "HELLO.TXT"));
                try (Stream file = fs.openFile("DIR" + FS + "HELLO.TXT", FileMode.Open)) {
                    byte[] read = new byte[content.length];
                    assertEquals(content.length, file.read(read, 0, read.length));
                    assertArrayEquals(content, read);
                }
            }
        }
    }

    @Test
    @DisplayName("a lun is addressable by its uri")
    void uri() throws Exception {
        try (IscsiTargetServer target = new IscsiTargetServer(image);
                Session session = new Initiator().connectTo(TARGET_NAME, target.getPortalAddress())) {
            LunInfo lun = session.getLuns()[0];
            List<String> uris = lun.getUris();
            assertFalse(uris.isEmpty());
            assertTrue(uris.get(0).startsWith("iscsi://"), uris.get(0));

            LunInfo parsed = LunInfo.parseUri(uris.get(0));
            assertEquals(lun.getLun(), parsed.getLun());
            assertEquals(TARGET_NAME, parsed.getTarget().getName());
        }
    }
}
