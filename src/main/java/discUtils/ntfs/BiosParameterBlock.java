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
import java.util.Random;

import discUtils.core.Geometry;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Sizes;


class BiosParameterBlock {
    static final String NTFS_OEM_ID = "NTFS    ";

    // Value: 0x80 (first hard disk)
    public byte _biosDriveNumber;

    private short _bytesPerSector;

    public int getBytesPerSector() {
        return _bytesPerSector & 0xffff;
    }

    // Value: 0x00
    public byte _chkDskFlags;

    // Must be 0
    public short _fatRootEntriesCount;

    // Must be 0
    public short _fatSize16;

    // Value: 0x3F 0x00 0x00 0x00
    public int _hiddenSectors;

    // Must be 0xF8
    public byte _media;

    public long _mftCluster;

    public long _mftMirrorCluster;

    // Must be 0
    public byte _numFats;

    // Value: 0xFF 0x00
    public short _numHeads;

    public String _oemId;

    // Value: 0x00
    public byte _paddingByte;

    public byte _rawIndexBufferSize;

    public byte _rawMftRecordSize;

    // Must be 0
    public short _reservedSectors;

    private byte _sectorsPerCluster;

    public int getSectorsPerCluster() {
        return _sectorsPerCluster & 0xff;
    }

    // Value: 0x3F 0x00
    public short _sectorsPerTrack;

    // Value: 0x80
    public byte _signatureByte;

    // Must be 0
    public short _totalSectors16;

    // Must be 0
    public int _totalSectors32;

    public long _totalSectors64;

    public long _volumeSerialNumber;

    public int getBytesPerCluster() {
        return getBytesPerSector() * getSectorsPerCluster();
    }

    public int getIndexBufferSize() {
        return calcRecordSize(_rawIndexBufferSize);
    }

    public int getMftRecordSize() {
        return calcRecordSize(_rawMftRecordSize);
    }

    public void dump(PrintWriter writer, String linePrefix) {
        writer.println(linePrefix + "BIOS PARAMETER BLOCK (BPB)");
        writer.println(linePrefix + "                OEM ID: " + _oemId);
        writer.println(linePrefix + "      Bytes per Sector: " + _bytesPerSector);
        writer.println(linePrefix + "   Sectors per Cluster: " + _sectorsPerCluster);
        writer.println(linePrefix + "      Reserved Sectors: " + _reservedSectors);
        writer.println(linePrefix + "                # FATs: " + _numFats);
        writer.println(linePrefix + "    # FAT Root Entries: " + _fatRootEntriesCount);
        writer.println(linePrefix + "   Total Sectors (16b): " + _totalSectors16);
        writer.println(linePrefix + "                 Media: " + Integer.toHexString(_media) + "h");
        writer.println(linePrefix + "        FAT size (16b): " + _fatSize16);
        writer.println(linePrefix + "     Sectors per Track: " + _sectorsPerTrack);
        writer.println(linePrefix + "               # Heads: " + _numHeads);
        writer.println(linePrefix + "        Hidden Sectors: " + _hiddenSectors);
        writer.println(linePrefix + "   Total Sectors (32b): " + _totalSectors32);
        writer.println(linePrefix + "     BIOS Drive Number: " + _biosDriveNumber);
        writer.println(linePrefix + "          Chkdsk Flags: " + _chkDskFlags);
        writer.println(linePrefix + "        Signature Byte: " + _signatureByte);
        writer.println(linePrefix + "   Total Sectors (64b): " + _totalSectors64);
        writer.println(linePrefix + "       MFT Record Size: " + _rawMftRecordSize);
        writer.println(linePrefix + "     Index buffer Size: " + _rawIndexBufferSize);
        writer.println(linePrefix + "  Volume Serial Number: " + _volumeSerialNumber);
    }

    static BiosParameterBlock initialized(Geometry diskGeometry,
                                                 int clusterSize,
                                                 int partitionStartLba,
                                                 long partitionSizeLba,
                                                 int mftRecordSize,
                                                 int indexBufferSize) {
        BiosParameterBlock bpb = new BiosParameterBlock();
        bpb._oemId = NTFS_OEM_ID;
        bpb._bytesPerSector = Sizes.Sector;
        bpb._sectorsPerCluster = (byte) (clusterSize / bpb.getBytesPerSector());
        bpb._reservedSectors = 0;
        bpb._numFats = 0;
        bpb._fatRootEntriesCount = 0;
        bpb._totalSectors16 = 0;
        bpb._media = (byte) 0xF8;
        bpb._fatSize16 = 0;
        bpb._sectorsPerTrack = (short) diskGeometry.getSectorsPerTrack();
        bpb._numHeads = (short) diskGeometry.getHeadsPerCylinder();
        bpb._hiddenSectors = partitionStartLba;
        bpb._totalSectors32 = 0;
        bpb._biosDriveNumber = (byte) 0x80;
        bpb._chkDskFlags = 0;
        bpb._signatureByte = (byte) 0x80;
        bpb._paddingByte = 0;
        bpb._totalSectors64 = partitionSizeLba - 1;
        bpb._rawMftRecordSize = bpb.codeRecordSize(mftRecordSize);
        bpb._rawIndexBufferSize = bpb.codeRecordSize(indexBufferSize);
        bpb._volumeSerialNumber = genSerialNumber();

        return bpb;
    }

    static BiosParameterBlock fromBytes(byte[] bytes, int offset) {
        BiosParameterBlock bpb = new BiosParameterBlock();
        bpb._oemId = EndianUtilities.bytesToString(bytes, offset + 0x03, 8);
        bpb._bytesPerSector = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x0B);
        bpb._totalSectors16 = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x13);
        bpb._totalSectors32 = EndianUtilities.toUInt32LittleEndian(bytes, offset + 0x20);
        bpb._signatureByte = bytes[offset + 0x26];
        bpb._totalSectors64 = EndianUtilities.toInt64LittleEndian(bytes, offset + 0x28);
        bpb._mftCluster = EndianUtilities.toInt64LittleEndian(bytes, offset + 0x30);
        bpb._rawMftRecordSize = bytes[offset + 0x40];
        bpb._sectorsPerCluster = bytes[offset + 0x0D];
        if (!bpb.isValid(Long.MAX_VALUE))
            return bpb;

        bpb._reservedSectors = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x0E);
        bpb._numFats = bytes[offset + 0x10];
        bpb._fatRootEntriesCount = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x11);
        bpb._media = bytes[offset + 0x15];
        bpb._fatSize16 = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x16);
        bpb._sectorsPerTrack = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x18);
        bpb._numHeads = EndianUtilities.toUInt16LittleEndian(bytes, offset + 0x1A);
        bpb._hiddenSectors = EndianUtilities.toUInt32LittleEndian(bytes, offset + 0x1C);
        bpb._biosDriveNumber = bytes[offset + 0x24];
        bpb._chkDskFlags = bytes[offset + 0x25];
        bpb._paddingByte = bytes[offset + 0x27];
        bpb._mftMirrorCluster = EndianUtilities.toInt64LittleEndian(bytes, offset + 0x38);
        bpb._rawIndexBufferSize = bytes[offset + 0x44];
        bpb._volumeSerialNumber = EndianUtilities.toUInt64LittleEndian(bytes, offset + 0x48);

        return bpb;
    }

    void toBytes(byte[] buffer, int offset) {
        EndianUtilities.stringToBytes(_oemId, buffer, offset + 0x03, 8);
        EndianUtilities.writeBytesLittleEndian(_bytesPerSector, buffer, offset + 0x0B);
        buffer[offset + 0x0D] = _sectorsPerCluster;
        EndianUtilities.writeBytesLittleEndian(_reservedSectors, buffer, offset + 0x0E);
        buffer[offset + 0x10] = _numFats;
        EndianUtilities.writeBytesLittleEndian(_fatRootEntriesCount, buffer, offset + 0x11);
        EndianUtilities.writeBytesLittleEndian(_totalSectors16, buffer, offset + 0x13);
        buffer[offset + 0x15] = _media;
        EndianUtilities.writeBytesLittleEndian(_fatSize16, buffer, offset + 0x16);
        EndianUtilities.writeBytesLittleEndian(_sectorsPerTrack, buffer, offset + 0x18);
        EndianUtilities.writeBytesLittleEndian(_numHeads, buffer, offset + 0x1A);
        EndianUtilities.writeBytesLittleEndian(_hiddenSectors, buffer, offset + 0x1C);
        EndianUtilities.writeBytesLittleEndian(_totalSectors32, buffer, offset + 0x20);
        buffer[offset + 0x24] = _biosDriveNumber;
        buffer[offset + 0x25] = _chkDskFlags;
        buffer[offset + 0x26] = _signatureByte;
        buffer[offset + 0x27] = _paddingByte;
        EndianUtilities.writeBytesLittleEndian(_totalSectors64, buffer, offset + 0x28);
        EndianUtilities.writeBytesLittleEndian(_mftCluster, buffer, offset + 0x30);
        EndianUtilities.writeBytesLittleEndian(_mftMirrorCluster, buffer, offset + 0x38);
        buffer[offset + 0x40] = _rawMftRecordSize;
        buffer[offset + 0x44] = _rawIndexBufferSize;
        EndianUtilities.writeBytesLittleEndian(_volumeSerialNumber, buffer, offset + 0x48);
    }

    int calcRecordSize(byte rawSize) {
        if ((rawSize & 0x80) != 0) {
            return 1 << -rawSize;
        }

        return rawSize * getSectorsPerCluster() * getBytesPerSector();
    }

    boolean isValidOemId() {
        return (_oemId != null && !_oemId.isEmpty() && _oemId.length() == NTFS_OEM_ID.length() &&
                _oemId.compareTo(NTFS_OEM_ID) == 0);
    }

    boolean isValid(long volumeSize) {
        // Some filesystem creation tools are not very strict and DO NOT
        // set the Signature byte to 0x80 (Version "8.0" NTFS BPB).
        //
        // Let's rather check OemId here, so we don't fail hard.
        if (!isValidOemId() || _totalSectors16 != 0 || _totalSectors32 != 0 || _totalSectors64 == 0 || getMftRecordSize() == 0 ||
            _mftCluster == 0 || _bytesPerSector == 0) {
            return false;
        }

        long mftPos = _mftCluster * getSectorsPerCluster() * getBytesPerSector();
        return mftPos < _totalSectors64 * getBytesPerSector() && mftPos < volumeSize;
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
