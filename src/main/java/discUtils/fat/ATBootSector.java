/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.fat;


import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;

import discUtils.core.Geometry;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;
import vavi.util.Debug;


/**
 * ATBootSector.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-08-06 nsano initial version <br>
 */
public class ATBootSector implements BootSector {

    private byte[] bootSector;

    private String oemName;
    private byte sectorsPerCluster;
    private byte fatCount;
    private byte media;
    private byte biosDriveNumber;
    private String fileSystemType;
    private FatType fatVariant;

    private short bpbBkBootSec;
    private short bpbBytesPerSec;
    private short bpbExtFlags;
    private short bpbFATSz16;
    private int bpbFATSz32;
    private short bpbFSInfo;
    private short bpbFSVer;
    private int bpbHiddSec;
    private short bpbNumHeads;
    private int bpbRootClus;
    private short bpbRootEntCnt;
    private short bpbRsvdSecCnt;
    private short bpbSecPerTrk;
    private short bpbTotSec16;
    private int bpbTotSec32;

    private byte bsBootSig;
    private int bsVolId;
    private String bsVolLab;

    public ATBootSector(Stream stream) {
        stream.position(0);
        bootSector = StreamUtilities.readSector(stream);
//Debug.println(Level.FINE, "\n" + StringUtil.getDump(bootSector, 64));
        fatVariant = detectFATType(bootSector);
        readBPB();
    }

    @Override
    public int getFatCount() {
        return fatCount & 0xff;
    }

    @Override
    public byte getActiveFat() {
        return (byte) ((bpbExtFlags & 0x08) != 0 ? bpbExtFlags & 0x7 : 0);
    }

    @Override
    public int getBytesPerSector() {
        return bpbBytesPerSec & 0xffff;
    }

    @Override
    public long getFatSize() {
        return bpbFATSz16 != 0 ? bpbFATSz16 & 0xffff : bpbFATSz32 & 0xffff_ffffL;
    }

    @Override
    public int getFatSize16() {
        return bpbFATSz16 & 0xffff;
    }

    @Override
    public FatType getFatVariant() {
        return fatVariant;
    }

    @Override
    public String getOemName() {
        return oemName;
    }

    @Override
    public int getReservedSectorCount() {
        return bpbRsvdSecCnt & 0xffff;
    }

    @Override
    public int getSectorsPerCluster() {
        return sectorsPerCluster & 0xff;
    }

    @Override
    public long getTotalSectors() {
        return bpbTotSec16 != 0 ? bpbTotSec16 & 0xffff : bpbTotSec32 & 0xffff_ffffL;
    }

    @Override
    public String getVolumeLabel() {
        return bsVolLab;
    }

    /**
     * Gets the Sector location of the backup boot sector (FAT32 only).
     */
    public int getBackupBootSector() {
        return bpbBkBootSec & 0xffff;
    }

    /**
     * Gets the BIOS drive number for BIOS Int 13h calls.
     */
    public int getBiosDriveNumber() {
        return biosDriveNumber & 0xff;
    }

    /**
     * Gets a value indicating whether the volumeId, VolumeLabel and
     * FileSystemType fields are valid.
     */
    public boolean getExtendedBootSignaturePresent() {
        return bsBootSig == 0x29;
    }

    /**
     * Gets the (informational only) file system type recorded in the meta-data.
     */
    public String getFileSystemType() {
        return fileSystemType;
    }

    /**
     * Gets the sector location of the FSINFO structure (FAT32 only).
     */
    public int getFSInfoSector() {
        return bpbFSInfo;
    }

    /**
     * Gets the number of logical heads.
     */
    public int getHeads() {
        return bpbNumHeads & 0xffff;
    }

    /**
     * Gets the number of hidden sectors, hiding partition tables, etc.
     */
    public long getHiddenSectors() {
        return bpbHiddSec;
    }

    @Override
    public int getMaxRootDirectoryEntries() {
        return bpbRootEntCnt & 0xffff;
    }

    /**
     * Gets the Media marker byte, which indicates fixed or removable media.
     */
    public byte getMedia() {
        return media;
    }

    /**
     * Gets a value indicating whether FAT changes are mirrored to all copies of
     * the FAT.
     */
    public boolean getMirrorFat() {
        return (bpbExtFlags & 0x08) == 0;
    }

    @Override
    public long getRootDirectoryCluster() {
        return bpbRootClus;
    }

    /**
     * Gets the number of sectors per logical track.
     */
    public int getSectorsPerTrack() {
        return bpbSecPerTrk & 0xffff;
    }

    /**
     * Gets the file-system version (usually 0).
     */
    public int getVersion() {
        return bpbFSVer;
    }

    /**
     * Gets the volume serial number.
     */
    public int getVolumeId() {
        return bsVolId;
    }

    private static FatType detectFATType(byte[] bpb) {
        int bpbBytesPerSec = EndianUtilities.toUInt16LittleEndian(bpb, 11) & 0xffff;
        if (bpbBytesPerSec == 0) {
            throw new IllegalStateException("Bytes per sector is 0, invalid or corrupt filesystem.");
        }

        int bpbRootEntCnt = EndianUtilities.toUInt16LittleEndian(bpb, 17) & 0xffff;
        int bpbFATSz16 = EndianUtilities.toUInt16LittleEndian(bpb, 22) & 0xffff;
        int bpbFATSz32 = EndianUtilities.toUInt32LittleEndian(bpb, 36);
        int bpbTotSec16 = EndianUtilities.toUInt16LittleEndian(bpb, 19) & 0xffff;
        int bpbTotSec32 = EndianUtilities.toUInt32LittleEndian(bpb, 32);
        int bpbResvdSecCnt = EndianUtilities.toUInt16LittleEndian(bpb, 14);
        int bpbNumFATs = bpb[16] & 0xff;
        int bpbSecPerClus = bpb[13] & 0xff;
        int rootDirSectors = (bpbRootEntCnt * 32 + bpbBytesPerSec - 1) / bpbBytesPerSec;
        int fatSz = bpbFATSz16 != 0 ? bpbFATSz16 : bpbFATSz32;
        int totalSec = bpbTotSec16 != 0 ? bpbTotSec16 : bpbTotSec32;
        int dataSec = totalSec - (bpbResvdSecCnt + bpbNumFATs * fatSz + rootDirSectors);
        int countOfClusters = dataSec / bpbSecPerClus;
//Debug.println(Level.FINE, "bpbBytesPerSec: " + bpbBytesPerSec);
//Debug.println(Level.FINE, "bpbRootEntCnt: " + bpbRootEntCnt);
//Debug.println(Level.FINE, "bpbFATSz16: " + bpbFATSz16);
//Debug.println(Level.FINE, "bpbFATSz32: " + bpbFATSz32);
//Debug.println(Level.FINE, "bpbTotSec16: " + bpbTotSec16);
//Debug.println(Level.FINE, "bpbTotSec32: " + bpbTotSec32);
//Debug.println(Level.FINE, "bpbNumFATs: " + bpbNumFATs);
//Debug.println(Level.FINE, "bpbSecPerClus: " + bpbSecPerClus);
//Debug.println(Level.FINE, "rootDirSectors: " + rootDirSectors);
//Debug.println(Level.FINE, "fatSz: " + fatSz);
//Debug.println(Level.FINE, "totalSec: " + totalSec);
//Debug.println(Level.FINE, "dataSec: " + dataSec);
//Debug.println(Level.FINE, "countOfClusters: " + countOfClusters);
        if (countOfClusters < 4085) {
            return FatType.Fat12;
        }

        if (countOfClusters < 65525) {
            return FatType.Fat16;
        }

        return FatType.Fat32;
    }

    private void readBPB() {
        oemName = new String(bootSector, 3, 8, StandardCharsets.US_ASCII).replaceFirst("\0*$", "");
        bpbBytesPerSec = EndianUtilities.toUInt16LittleEndian(bootSector, 11);
        sectorsPerCluster = bootSector[13];
        bpbRsvdSecCnt = EndianUtilities.toUInt16LittleEndian(bootSector, 14);
        fatCount = bootSector[16];
        bpbRootEntCnt = EndianUtilities.toUInt16LittleEndian(bootSector, 17);
        bpbTotSec16 = EndianUtilities.toUInt16LittleEndian(bootSector, 19);
        media = bootSector[21];
        bpbFATSz16 = EndianUtilities.toUInt16LittleEndian(bootSector, 22);
        bpbSecPerTrk = EndianUtilities.toUInt16LittleEndian(bootSector, 24);
        bpbNumHeads = EndianUtilities.toUInt16LittleEndian(bootSector, 26);
        bpbHiddSec = EndianUtilities.toUInt32LittleEndian(bootSector, 28);
        bpbTotSec32 = EndianUtilities.toUInt32LittleEndian(bootSector, 32);
        if (fatVariant != FatType.Fat32) {
            readBS(36);
        } else {
            bpbFATSz32 = EndianUtilities.toUInt32LittleEndian(bootSector, 36);
            bpbExtFlags = EndianUtilities.toUInt16LittleEndian(bootSector, 40);
            bpbFSVer = EndianUtilities.toUInt16LittleEndian(bootSector, 42);
            bpbRootClus = EndianUtilities.toUInt32LittleEndian(bootSector, 44);
            bpbFSInfo = EndianUtilities.toUInt16LittleEndian(bootSector, 48);
            bpbBkBootSec = EndianUtilities.toUInt16LittleEndian(bootSector, 50);
            readBS(64);
        }
//Debug.println(Level.FINE, "bpbBytesPerSec: " + bpbBytesPerSec);
//Debug.println(Level.FINE, "sectorsPerCluster: " + getSectorsPerCluster());
//Debug.println(Level.FINE, "bpbRsvdSecCnt: " + bpbRsvdSecCnt);
//Debug.println(Level.FINE, "fatCount: " + getFatCount());
//Debug.println(Level.FINE, "bpbRootEntCnt: " + bpbRootEntCnt);
//Debug.println(Level.FINE, "bpbTotSec16: " + bpbTotSec16);
Debug.printf(Level.FINE, "media: %02x\n", media);
//Debug.println(Level.FINE, "bpbFATSz16: " + bpbFATSz16);
//Debug.println(Level.FINE, "bpbSecPerTrk: " + bpbSecPerTrk);
//Debug.println(Level.FINE, "bpbNumHeads: " + bpbNumHeads);
//Debug.println(Level.FINE, "bpbHiddSec: " + bpbHiddSec);
//Debug.println(Level.FINE, "bpbTotSec32: " + bpbTotSec32);
    }

    private void readBS(int offset) {
        biosDriveNumber = bootSector[offset];
        bsBootSig = bootSector[offset + 2];
        bsVolId = EndianUtilities.toUInt32LittleEndian(bootSector, offset + 3);
        bsVolLab = new String(bootSector, offset + 7, 11, StandardCharsets.US_ASCII);
        fileSystemType = new String(bootSector, offset + 18, 8, StandardCharsets.US_ASCII);
    }

    /**
     * Writes a FAT12/FAT16 BPB.
     *
     * @param bootSector The buffer to fill.
     * @param sectors The total capacity of the disk (in sectors).
     * @param fatType The number of bits in each FAT entry.
     * @param maxRootEntries The maximum number of root directory entries.
     * @param hiddenSectors The number of hidden sectors before this file system
     *            (i.e. partition offset).
     * @param reservedSectors The number of reserved sectors before the FAT.
     * @param sectorsPerCluster The number of sectors per cluster.
     * @param diskGeometry The geometry of the disk containing the fat file
     *            system.
     * @param isFloppy Indicates if the disk is a removable media (a floppy
     *            disk).
     * @param volId The disk's volume Id.
     * @param label The disk's label (or null).
     */
    static void writeBPB(byte[] bootSector,
                                 int sectors,
                                 FatType fatType,
                                 short maxRootEntries,
                                 int hiddenSectors,
                                 short reservedSectors,
                                 byte sectorsPerCluster,
                                 Geometry diskGeometry,
                                 boolean isFloppy,
                                 int volId,
                                 String label) {
        int fatSectors = calcFatSize(sectors, fatType, sectorsPerCluster);
        bootSector[0] = (byte) 0xEB;
        bootSector[1] = 0x3C;
        bootSector[2] = (byte) 0x90;
        // OEM Name
        EndianUtilities.stringToBytes("DISCUTIL", bootSector, 3, 8);
        // Bytes Per Sector (512)
        bootSector[11] = 0;
        bootSector[12] = 2;
        // Sectors Per Cluster
        bootSector[13] = sectorsPerCluster;
        // Reserved Sector Count
        EndianUtilities.writeBytesLittleEndian(reservedSectors, bootSector, 14);
        // Number of FATs
        bootSector[16] = 2;
        // Number of Entries in the root directory
        EndianUtilities.writeBytesLittleEndian(maxRootEntries, bootSector, 17);
        // Total number of sectors (small)
        EndianUtilities.writeBytesLittleEndian((short) (sectors < 0x10000 ? sectors : 0), bootSector, 19);
        // Media
        bootSector[21] = (byte) (isFloppy ? 0xF0 : 0xF8);
        // FAT size (FAT12/FAT16)
        EndianUtilities.writeBytesLittleEndian((short) (fatType.getValue() < FatType.Fat32.getValue() ? fatSectors : 0),
                bootSector,
                22);
        // Sectors Per Track
        EndianUtilities.writeBytesLittleEndian((short) diskGeometry.getSectorsPerTrack(), bootSector, 24);
        // Heads Per Cylinder
        EndianUtilities.writeBytesLittleEndian((short) diskGeometry.getHeadsPerCylinder(), bootSector, 26);
        // Hidden Sectors
        EndianUtilities.writeBytesLittleEndian(hiddenSectors, bootSector, 28);
        // Total number of sectors (large)
        EndianUtilities.writeBytesLittleEndian(sectors >= 0x10000 ? sectors : 0, bootSector, 32);
        if (fatType.getValue() < FatType.Fat32.getValue()) {
            writeBS(bootSector, 36, isFloppy, volId, label, fatType);
        } else {
            // FAT size (FAT32)
            EndianUtilities.writeBytesLittleEndian(fatSectors, bootSector, 36);
            // ext flags: 0x80 = FAT 1 (i.e. Zero) active, mirroring
            bootSector[40] = 0x00;
            bootSector[41] = 0x00;
            // Filesystem version (0.0)
            bootSector[42] = 0;
            bootSector[43] = 0;
            // First cluster of the root directory, always 2 since we don't do
            // bad
            // sectors...
            EndianUtilities.writeBytesLittleEndian(2, bootSector, 44);
            // Sector number of FSINFO
            EndianUtilities.writeBytesLittleEndian(1, bootSector, 48);
            // Sector number of the Backup Boot Sector
            EndianUtilities.writeBytesLittleEndian(6, bootSector, 50);
            // Reserved area - must be set to 0
            Arrays.fill(bootSector, 52, 52 + 12, (byte) 0);
            writeBS(bootSector, 64, isFloppy, volId, label, fatType);
        }
        bootSector[510] = 0x55;
        bootSector[511] = (byte) 0xAA;
    }

    static int calcFatSize(int sectors, FatType fatType, byte sectorsPerCluster) {
        int numClusters = sectors / sectorsPerCluster;
        int fatBytes = numClusters * (short) fatType.ordinal() / 8;
        return (fatBytes + Sizes.Sector - 1) / Sizes.Sector;
    }

    private static void writeBS(byte[] bootSector, int offset, boolean isFloppy, int volId, String label, FatType fatType) {
        if (label == null || label.isEmpty()) {
            label = "NO NAME    ";
        }

        String fsType = "FAT32   ";
        if (fatType == FatType.Fat12) {
            fsType = "FAT12   ";
        } else if (fatType == FatType.Fat16) {
            fsType = "FAT16   ";
        }

        // Drive Number (for BIOS)
        bootSector[offset + 0] = (byte) (isFloppy ? 0x00 : 0x80);
        // Reserved
        bootSector[offset + 1] = 0;
        // Boot Signature (indicates next 3 fields present)
        bootSector[offset + 2] = 0x29;
        // Volume Id
        EndianUtilities.writeBytesLittleEndian(volId, bootSector, offset + 3);
        // Volume Label
        EndianUtilities.stringToBytes(label + "           ", bootSector, offset + 7, 11);
        // File System Type
        EndianUtilities.stringToBytes(fsType, bootSector, offset + 18, 8);
    }
}
