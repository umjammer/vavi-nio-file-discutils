//
// Copyright (c) 2008-2011, Kenneth Bell
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package discUtils.ntfs;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import discUtils.core.Geometry;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Sizes;
import vavi.util.ByteUtil;


class BiosParameterBlock {

    static final String NTFS_OEM_ID = "NTFS    ";

    // Value: 0x80 (first hard disk)
    public byte biosDriveNumber;

    private short bytesPerSector;

    public int getBytesPerSector() {
        return bytesPerSector & 0xffff;
    }

    // Value: 0x00
    public byte chkDskFlags;

    // Must be 0
    public short fatRootEntriesCount;

    // Must be 0
    public short fatSize16;

    // Value: 0x3F 0x00 0x00 0x00
    public int hiddenSectors;

    // Must be 0xF8
    public byte media;

    public long mftCluster;

    public long mftMirrorCluster;

    // Must be 0
    public byte numFats;

    // Value: 0xFF 0x00
    public short numHeads;

    public String oemId;

    // Value: 0x00
    public byte paddingByte;

    public byte rawIndexBufferSize;

    public byte rawMftRecordSize;

    // Must be 0
    public short reservedSectors;

    private byte sectorsPerCluster;

    public int getSectorsPerCluster() {
        return sectorsPerCluster & 0xff;
    }

    // Value: 0x3F 0x00
    public short sectorsPerTrack;

    // Value: 0x80
    public byte signatureByte;

    // Must be 0
    public short totalSectors16;

    // Must be 0
    public int totalSectors32;

    public long totalSectors64;

    public long volumeSerialNumber;

    public int getBytesPerCluster() {
        return getBytesPerSector() * getSectorsPerCluster();
    }

    public int getIndexBufferSize() {
        return calcRecordSize(rawIndexBufferSize);
    }

    public int getMftRecordSize() {
        return calcRecordSize(rawMftRecordSize);
    }

    public void dump(PrintWriter writer, String linePrefix) {
        writer.println(linePrefix + "BIOS PARAMETER BLOCK (BPB)");
        writer.println(linePrefix + "                OEM ID: " + oemId);
        writer.println(linePrefix + "      Bytes per Sector: " + bytesPerSector);
        writer.println(linePrefix + "   Sectors per Cluster: " + sectorsPerCluster);
        writer.println(linePrefix + "      Reserved Sectors: " + reservedSectors);
        writer.println(linePrefix + "                # FATs: " + numFats);
        writer.println(linePrefix + "    # FAT Root Entries: " + fatRootEntriesCount);
        writer.println(linePrefix + "   Total Sectors (16b): " + totalSectors16);
        writer.println(linePrefix + "                 Media: " + Integer.toHexString(media) + "h");
        writer.println(linePrefix + "        FAT size (16b): " + fatSize16);
        writer.println(linePrefix + "     Sectors per Track: " + sectorsPerTrack);
        writer.println(linePrefix + "               # Heads: " + numHeads);
        writer.println(linePrefix + "        Hidden Sectors: " + hiddenSectors);
        writer.println(linePrefix + "   Total Sectors (32b): " + totalSectors32);
        writer.println(linePrefix + "     BIOS Drive Number: " + biosDriveNumber);
        writer.println(linePrefix + "          Chkdsk flags: " + chkDskFlags);
        writer.println(linePrefix + "        Signature Byte: " + signatureByte);
        writer.println(linePrefix + "   Total Sectors (64b): " + totalSectors64);
        writer.println(linePrefix + "       MFT Record Size: " + rawMftRecordSize);
        writer.println(linePrefix + "     Index buffer Size: " + rawIndexBufferSize);
        writer.println(linePrefix + "  Volume Serial Number: " + volumeSerialNumber);
    }

    static BiosParameterBlock initialized(Geometry diskGeometry,
                                                 int clusterSize,
                                                 int partitionStartLba,
                                                 long partitionSizeLba,
                                                 int mftRecordSize,
                                                 int indexBufferSize) {
        BiosParameterBlock bpb = new BiosParameterBlock();
        bpb.oemId = NTFS_OEM_ID;
        bpb.bytesPerSector = Sizes.Sector;
        bpb.sectorsPerCluster = (byte) (clusterSize / bpb.getBytesPerSector());
        bpb.reservedSectors = 0;
        bpb.numFats = 0;
        bpb.fatRootEntriesCount = 0;
        bpb.totalSectors16 = 0;
        bpb.media = (byte) 0xF8;
        bpb.fatSize16 = 0;
        bpb.sectorsPerTrack = (short) diskGeometry.getSectorsPerTrack();
        bpb.numHeads = (short) diskGeometry.getHeadsPerCylinder();
        bpb.hiddenSectors = partitionStartLba;
        bpb.totalSectors32 = 0;
        bpb.biosDriveNumber = (byte) 0x80;
        bpb.chkDskFlags = 0;
        bpb.signatureByte = (byte) 0x80;
        bpb.paddingByte = 0;
        bpb.totalSectors64 = partitionSizeLba - 1;
        bpb.rawMftRecordSize = bpb.codeRecordSize(mftRecordSize);
        bpb.rawIndexBufferSize = bpb.codeRecordSize(indexBufferSize);
        bpb.volumeSerialNumber = genSerialNumber();

        return bpb;
    }

    static BiosParameterBlock fromBytes(byte[] bytes, int offset) {
        BiosParameterBlock bpb = new BiosParameterBlock();
        bpb.oemId = new String(bytes, offset + 0x03, 8, StandardCharsets.US_ASCII);
        bpb.bytesPerSector = ByteUtil.readLeShort(bytes, offset + 0x0B);
        bpb.totalSectors16 = ByteUtil.readLeShort(bytes, offset + 0x13);
        bpb.totalSectors32 = ByteUtil.readLeInt(bytes, offset + 0x20);
        bpb.signatureByte = bytes[offset + 0x26];
        bpb.totalSectors64 = ByteUtil.readLeLong(bytes, offset + 0x28);
        bpb.mftCluster = ByteUtil.readLeLong(bytes, offset + 0x30);
        bpb.rawMftRecordSize = bytes[offset + 0x40];
        bpb.sectorsPerCluster = bytes[offset + 0x0D];
        if (!bpb.isValid(Long.MAX_VALUE))
            return bpb;

        bpb.reservedSectors = ByteUtil.readLeShort(bytes, offset + 0x0E);
        bpb.numFats = bytes[offset + 0x10];
        bpb.fatRootEntriesCount = ByteUtil.readLeShort(bytes, offset + 0x11);
        bpb.media = bytes[offset + 0x15];
        bpb.fatSize16 = ByteUtil.readLeShort(bytes, offset + 0x16);
        bpb.sectorsPerTrack = ByteUtil.readLeShort(bytes, offset + 0x18);
        bpb.numHeads = ByteUtil.readLeShort(bytes, offset + 0x1A);
        bpb.hiddenSectors = ByteUtil.readLeInt(bytes, offset + 0x1C);
        bpb.biosDriveNumber = bytes[offset + 0x24];
        bpb.chkDskFlags = bytes[offset + 0x25];
        bpb.paddingByte = bytes[offset + 0x27];
        bpb.mftMirrorCluster = ByteUtil.readLeLong(bytes, offset + 0x38);
        bpb.rawIndexBufferSize = bytes[offset + 0x44];
        bpb.volumeSerialNumber = ByteUtil.readLeLong(bytes, offset + 0x48);

        return bpb;
    }

    void toBytes(byte[] buffer, int offset) {
        EndianUtilities.stringToBytes(oemId, buffer, offset + 0x03, 8);
        ByteUtil.writeLeShort(bytesPerSector, buffer, offset + 0x0B);
        buffer[offset + 0x0D] = sectorsPerCluster;
        ByteUtil.writeLeShort(reservedSectors, buffer, offset + 0x0E);
        buffer[offset + 0x10] = numFats;
        ByteUtil.writeLeShort(fatRootEntriesCount, buffer, offset + 0x11);
        ByteUtil.writeLeShort(totalSectors16, buffer, offset + 0x13);
        buffer[offset + 0x15] = media;
        ByteUtil.writeLeShort(fatSize16, buffer, offset + 0x16);
        ByteUtil.writeLeShort(sectorsPerTrack, buffer, offset + 0x18);
        ByteUtil.writeLeShort(numHeads, buffer, offset + 0x1A);
        ByteUtil.writeLeInt(hiddenSectors, buffer, offset + 0x1C);
        ByteUtil.writeLeInt(totalSectors32, buffer, offset + 0x20);
        buffer[offset + 0x24] = biosDriveNumber;
        buffer[offset + 0x25] = chkDskFlags;
        buffer[offset + 0x26] = signatureByte;
        buffer[offset + 0x27] = paddingByte;
        ByteUtil.writeLeLong(totalSectors64, buffer, offset + 0x28);
        ByteUtil.writeLeLong(mftCluster, buffer, offset + 0x30);
        ByteUtil.writeLeLong(mftMirrorCluster, buffer, offset + 0x38);
        buffer[offset + 0x40] = rawMftRecordSize;
        buffer[offset + 0x44] = rawIndexBufferSize;
        ByteUtil.writeLeLong(volumeSerialNumber, buffer, offset + 0x48);
    }

    int calcRecordSize(byte rawSize) {
        if ((rawSize & 0x80) != 0) {
            return 1 << -rawSize;
        }

        return rawSize * getSectorsPerCluster() * getBytesPerSector();
    }

    boolean isValidOemId() {
        return (oemId != null && !oemId.isEmpty() && oemId.length() == NTFS_OEM_ID.length() &&
                oemId.compareTo(NTFS_OEM_ID) == 0);
    }

    boolean isValid(long volumeSize) {
        // Some filesystem creation tools are not very strict and DO NOT
        // set the Signature byte to 0x80 (Version "8.0" NTFS BPB).
        //
        // Let's rather check OemId here, so we don't fail hard.
        if (!isValidOemId() || totalSectors16 != 0 || totalSectors32 != 0 || totalSectors64 == 0 || getMftRecordSize() == 0 ||
            mftCluster == 0 || bytesPerSector == 0) {
            return false;
        }

        long mftPos = mftCluster * getSectorsPerCluster() * getBytesPerSector();
        return mftPos < totalSectors64 * getBytesPerSector() && mftPos < volumeSize;
    }

    private static long genSerialNumber() {
        byte[] buffer = new byte[8];
        Random rng = new Random();
        rng.nextBytes(buffer);
        return ByteUtil.readLeLong(buffer, 0);
    }

    private byte codeRecordSize(int size) {
        if (size >= getBytesPerCluster()) {
            return (byte) (size / getBytesPerCluster());
        }
        byte val = 0;
        while (size != 1) {
            size = (size >>> 1) & 0x7FFFFFFF;
            val++;
        }
        return (byte) -val;
    }
}
