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

package DiscUtils.Ntfs;

import java.io.PrintWriter;
import java.util.Random;

import DiscUtils.Core.Geometry;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Sizes;


public class BiosParameterBlock {
    static final String NTFS_OEM_ID = "NTFS    ";

    public byte BiosDriveNumber;

    // Value: 0x80 (first hard disk)
    public short BytesPerSector;

    public byte ChkDskFlags;

    // Value: 0x00
    public short FatRootEntriesCount;

    // Must be 0
    public short FatSize16;

    // Must be 0
    public int HiddenSectors;

    // Value: 0x3F 0x00 0x00 0x00
    public byte Media;

    // Must be 0xF8
    public long MftCluster;

    public long MftMirrorCluster;

    public byte NumFats;

    // Must be 0
    public short NumHeads;

    // Value: 0xFF 0x00
    public String OemId;

    public byte PaddingByte;

    // Value: 0x00
    public byte RawIndexBufferSize;

    public byte RawMftRecordSize;

    public short ReservedSectors;

    // Must be 0
    public byte SectorsPerCluster;

    public short SectorsPerTrack;

    // Value: 0x3F 0x00
    public byte SignatureByte;

    // Value: 0x80
    public short TotalSectors16;

    // Must be 0
    public int TotalSectors32;

    // Must be 0
    public long TotalSectors64;

    public long VolumeSerialNumber;

    public int getBytesPerCluster() {
        return BytesPerSector * SectorsPerCluster;
    }

    public int getIndexBufferSize() {
        return calcRecordSize(RawIndexBufferSize);
    }

    public int getMftRecordSize() {
        return calcRecordSize(RawMftRecordSize);
    }

    public void dump(PrintWriter writer, String linePrefix) {
        writer.println(linePrefix + "BIOS PARAMETER BLOCK (BPB)");
        writer.println(linePrefix + "                OEM ID: " + OemId);
        writer.println(linePrefix + "      Bytes per Sector: " + BytesPerSector);
        writer.println(linePrefix + "   Sectors per Cluster: " + SectorsPerCluster);
        writer.println(linePrefix + "      Reserved Sectors: " + ReservedSectors);
        writer.println(linePrefix + "                # FATs: " + NumFats);
        writer.println(linePrefix + "    # FAT Root Entries: " + FatRootEntriesCount);
        writer.println(linePrefix + "   Total Sectors (16b): " + TotalSectors16);
        writer.println(linePrefix + "                 Media: " + Integer.toHexString(Media) + "h");
        writer.println(linePrefix + "        FAT size (16b): " + FatSize16);
        writer.println(linePrefix + "     Sectors per Track: " + SectorsPerTrack);
        writer.println(linePrefix + "               # Heads: " + NumHeads);
        writer.println(linePrefix + "        Hidden Sectors: " + HiddenSectors);
        writer.println(linePrefix + "   Total Sectors (32b): " + TotalSectors32);
        writer.println(linePrefix + "     BIOS Drive Number: " + BiosDriveNumber);
        writer.println(linePrefix + "          Chkdsk Flags: " + ChkDskFlags);
        writer.println(linePrefix + "        Signature Byte: " + SignatureByte);
        writer.println(linePrefix + "   Total Sectors (64b): " + TotalSectors64);
        writer.println(linePrefix + "       MFT Record Size: " + RawMftRecordSize);
        writer.println(linePrefix + "     Index Buffer Size: " + RawIndexBufferSize);
        writer.println(linePrefix + "  Volume Serial Number: " + VolumeSerialNumber);
    }

    public static BiosParameterBlock initialized(Geometry diskGeometry,
                                                 int clusterSize,
                                                 int partitionStartLba,
                                                 long partitionSizeLba,
                                                 int mftRecordSize,
                                                 int indexBufferSize) {
        BiosParameterBlock bpb = new BiosParameterBlock();
        bpb.OemId = NTFS_OEM_ID;
        bpb.BytesPerSector = Sizes.Sector;
        bpb.SectorsPerCluster = (byte) (clusterSize / bpb.BytesPerSector);
        bpb.ReservedSectors = 0;
        bpb.NumFats = 0;
        bpb.FatRootEntriesCount = 0;
        bpb.TotalSectors16 = 0;
        bpb.Media = (byte) 0xF8;
        bpb.FatSize16 = 0;
        bpb.SectorsPerTrack = (short) diskGeometry.getSectorsPerTrack();
        bpb.NumHeads = (short) diskGeometry.getHeadsPerCylinder();
        bpb.HiddenSectors = partitionStartLba;
        bpb.TotalSectors32 = 0;
        bpb.BiosDriveNumber = (byte) 0x80;
        bpb.ChkDskFlags = 0;
        bpb.SignatureByte = (byte) 0x80;
        bpb.PaddingByte = 0;
        bpb.TotalSectors64 = partitionSizeLba - 1;
        bpb.RawMftRecordSize = bpb.codeRecordSize(mftRecordSize);
        bpb.RawIndexBufferSize = bpb.codeRecordSize(indexBufferSize);
        bpb.VolumeSerialNumber = genSerialNumber();
        return bpb;
    }

    public static BiosParameterBlock fromBytes(byte[] bytes, int offset) {
        BiosParameterBlock bpb = new BiosParameterBlock();
        bpb.OemId = EndianUtilities.bytesToString(bytes, offset + 0x03, 8);
        bpb.BytesPerSector = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x0B);
        bpb.TotalSectors16 = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x13);
        bpb.TotalSectors32 = EndianUtilities.toUInt32LittleEndian(bytes, offset + 0x20);
        bpb.SignatureByte = bytes[offset + 0x26];
        bpb.TotalSectors64 = EndianUtilities.toInt64LittleEndian(bytes, offset + 0x28);
        bpb.MftCluster = EndianUtilities.toInt64LittleEndian(bytes, offset + 0x30);
        bpb.RawMftRecordSize = bytes[offset + 0x40];
        bpb.SectorsPerCluster = bytes[offset + 0x0D];
        if (!bpb.isValid(Long.MAX_VALUE))
            return bpb;

        bpb.ReservedSectors = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x0E);
        bpb.NumFats = bytes[offset + 0x10];
        bpb.FatRootEntriesCount = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x11);
        bpb.Media = bytes[offset + 0x15];
        bpb.FatSize16 = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x16);
        bpb.SectorsPerTrack = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x18);
        bpb.NumHeads = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x1A);
        bpb.HiddenSectors = EndianUtilities.toUInt32LittleEndian(bytes, offset + 0x1C);
        bpb.BiosDriveNumber = bytes[offset + 0x24];
        bpb.ChkDskFlags = bytes[offset + 0x25];
        bpb.PaddingByte = bytes[offset + 0x27];
        bpb.MftMirrorCluster = EndianUtilities.toInt64LittleEndian(bytes, offset + 0x38);
        bpb.RawIndexBufferSize = bytes[offset + 0x44];
        bpb.VolumeSerialNumber = EndianUtilities.toUInt64LittleEndian(bytes, offset + 0x48);
        return bpb;
    }

    public void toBytes(byte[] buffer, int offset) {
        EndianUtilities.stringToBytes(OemId, buffer, offset + 0x03, 8);
        EndianUtilities.writeBytesLittleEndian(BytesPerSector, buffer, offset + 0x0B);
        buffer[offset + 0x0D] = SectorsPerCluster;
        EndianUtilities.writeBytesLittleEndian(ReservedSectors, buffer, offset + 0x0E);
        buffer[offset + 0x10] = NumFats;
        EndianUtilities.writeBytesLittleEndian(FatRootEntriesCount, buffer, offset + 0x11);
        EndianUtilities.writeBytesLittleEndian(TotalSectors16, buffer, offset + 0x13);
        buffer[offset + 0x15] = Media;
        EndianUtilities.writeBytesLittleEndian(FatSize16, buffer, offset + 0x16);
        EndianUtilities.writeBytesLittleEndian(SectorsPerTrack, buffer, offset + 0x18);
        EndianUtilities.writeBytesLittleEndian(NumHeads, buffer, offset + 0x1A);
        EndianUtilities.writeBytesLittleEndian(HiddenSectors, buffer, offset + 0x1C);
        EndianUtilities.writeBytesLittleEndian(TotalSectors32, buffer, offset + 0x20);
        buffer[offset + 0x24] = BiosDriveNumber;
        buffer[offset + 0x25] = ChkDskFlags;
        buffer[offset + 0x26] = SignatureByte;
        buffer[offset + 0x27] = PaddingByte;
        EndianUtilities.writeBytesLittleEndian(TotalSectors64, buffer, offset + 0x28);
        EndianUtilities.writeBytesLittleEndian(MftCluster, buffer, offset + 0x30);
        EndianUtilities.writeBytesLittleEndian(MftMirrorCluster, buffer, offset + 0x38);
        buffer[offset + 0x40] = RawMftRecordSize;
        buffer[offset + 0x44] = RawIndexBufferSize;
        EndianUtilities.writeBytesLittleEndian(VolumeSerialNumber, buffer, offset + 0x48);
    }

    public int calcRecordSize(byte rawSize) {
        if ((rawSize & 0x80) != 0) {
            return 1 << -rawSize;
        }

        return rawSize * SectorsPerCluster * BytesPerSector;
    }

    public boolean isValidOemId() {
        return (OemId != null && !OemId.isEmpty() && OemId.length() == NTFS_OEM_ID.length() &&
                OemId.compareTo(NTFS_OEM_ID) == 0);
    }

    public boolean isValid(long volumeSize) {
        /* Some filesystem creation tools are not very strict and DO NOT
         * set the Signature byte to 0x80 (Version "8.0" NTFS BPB).
         * Let's rather check OemId here, so we don't fail hard. */
        if (!isValidOemId() || TotalSectors16 != 0 || TotalSectors32 != 0 || TotalSectors64 == 0 || getMftRecordSize() == 0 ||
            MftCluster == 0 || BytesPerSector == 0) {
            return false;
        }

        long mftPos = MftCluster * SectorsPerCluster * BytesPerSector;
        return mftPos < TotalSectors64 * BytesPerSector && mftPos < volumeSize;
    }

    private static long genSerialNumber() {
        byte[] buffer = new byte[8];
        Random rng = new Random();
        rng.nextBytes(buffer);
        return EndianUtilities.toUInt64LittleEndian(buffer, 0);
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
