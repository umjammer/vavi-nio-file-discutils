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

package DiscUtils.Vhd;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import DiscUtils.Core.Geometry;
import DiscUtils.Streams.Util.EndianUtilities;


public class Footer {
    public static final String FileCookie = "conectix";

    public static final int FeatureNone = 0x0;

    public static final int FeatureTemporary = 0x1;

    public static final int FeatureReservedMustBeSet = 0x2;

    public static final int Version1 = 0x00010000;

    public static final int Version6Point1 = 0x00060001;

    public static final String VirtualPCSig = "vpc ";

    public static final String VirtualServerSig = "vs  ";

    public static final int VirtualPC2004Version = 0x00050000;

    public static final int VirtualServer2004Version = 0x00010000;

    public static final String WindowsHostOS = "Wi2k";

    public static final String MacHostOS = "Mac ";

    public static final int CylindersMask = 0x0000FFFF;

    public static final int HeadsMask = 0x00FF0000;

    public static final int SectorsMask = 0xFF000000;

    public static final Instant EpochUtc = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant();

    public int Checksum;

    public String Cookie;

    public String CreatorApp;

    public String CreatorHostOS;

    public int CreatorVersion;

    public long CurrentSize;

    public long DataOffset;

    public FileType DiskType = FileType.None;

    public int Features;

    public int FileFormatVersion;

    public Geometry Geometry;

    public long OriginalSize;

    public byte SavedState;

    public long Timestamp;

    public UUID UniqueId;

    public Footer(Geometry geometry, long capacity, FileType type) {
        Cookie = FileCookie;
        Features = FeatureReservedMustBeSet;
        FileFormatVersion = Version1;
        DataOffset = -1;
        Timestamp = System.currentTimeMillis();
        CreatorApp = "dutl";
        CreatorVersion = Version6Point1;
        CreatorHostOS = WindowsHostOS;
        OriginalSize = capacity;
        CurrentSize = capacity;
        Geometry = geometry;
        DiskType = type;
        UniqueId = UUID.randomUUID();
    }

    /**
     * /SavedState = 0;
     */
    public Footer(Footer toCopy) {
        Cookie = toCopy.Cookie;
        Features = toCopy.Features;
        FileFormatVersion = toCopy.FileFormatVersion;
        DataOffset = toCopy.DataOffset;
        Timestamp = toCopy.Timestamp;
        CreatorApp = toCopy.CreatorApp;
        CreatorVersion = toCopy.CreatorVersion;
        CreatorHostOS = toCopy.CreatorHostOS;
        OriginalSize = toCopy.OriginalSize;
        CurrentSize = toCopy.CurrentSize;
        Geometry = toCopy.Geometry;
        DiskType = toCopy.DiskType;
        Checksum = toCopy.Checksum;
        UniqueId = toCopy.UniqueId;
        SavedState = toCopy.SavedState;
    }

    private Footer() {
    }

    public boolean isValid() {
System.err.println(FileCookie.equals(Cookie) + ", " + isChecksumValid() + ", " + (FileFormatVersion == Version1));
        return FileCookie.equals(Cookie) && isChecksumValid()
        // && ((Features & FeatureReservedMustBeSet) != 0)
            && FileFormatVersion == Version1;
    }

    public boolean isChecksumValid() {
        return Checksum == calculateChecksum();
    }

    public int updateChecksum() {
        Checksum = calculateChecksum();
        return Checksum;
    }

    private int calculateChecksum() {
        Footer copy = new Footer(this);
        copy.Checksum = 0;
        byte[] asBytes = new byte[512];
        copy.toBytes(asBytes, 0);
        int checksum = 0;
        for (int value : asBytes) {
            checksum += value & 0xff;
        }
        checksum = ~checksum;
        return checksum;
    }

    public static Footer fromBytes(byte[] buffer, int offset) {
        Footer result = new Footer();
        result.Cookie = EndianUtilities.bytesToString(buffer, offset + 0, 8);
        result.Features = EndianUtilities.toUInt32BigEndian(buffer, offset + 8);
        result.FileFormatVersion = EndianUtilities.toUInt32BigEndian(buffer, offset + 12);
        result.DataOffset = EndianUtilities.toInt64BigEndian(buffer, offset + 16);
        result.Timestamp = EpochUtc.plusSeconds(EndianUtilities.toUInt32BigEndian(buffer, offset + 24)).toEpochMilli();
        result.CreatorApp = EndianUtilities.bytesToString(buffer, offset + 28, 4);
        result.CreatorVersion = EndianUtilities.toUInt32BigEndian(buffer, offset + 32);
        result.CreatorHostOS = EndianUtilities.bytesToString(buffer, offset + 36, 4);
        result.OriginalSize = EndianUtilities.toInt64BigEndian(buffer, offset + 40);
        result.CurrentSize = EndianUtilities.toInt64BigEndian(buffer, offset + 48);
        result.Geometry = new Geometry(EndianUtilities.toUInt16BigEndian(buffer, offset + 56) & 0xffff,
                                       buffer[58] & 0xff,
                                       buffer[59] & 0xff);
        result.DiskType = FileType.valueOf(EndianUtilities.toUInt32BigEndian(buffer, offset + 60));
        result.Checksum = EndianUtilities.toUInt32BigEndian(buffer, offset + 64);
        result.UniqueId = EndianUtilities.toGuidBigEndian(buffer, offset + 68);
        result.SavedState = buffer[84];
        return result;
    }

    public void toBytes(byte[] buffer, int offset) {
        EndianUtilities.stringToBytes(Cookie, buffer, offset + 0, 8);
        EndianUtilities.writeBytesBigEndian(Features, buffer, offset + 8);
        EndianUtilities.writeBytesBigEndian(FileFormatVersion, buffer, offset + 12);
        EndianUtilities.writeBytesBigEndian(DataOffset, buffer, offset + 16);
        EndianUtilities.writeBytesBigEndian((int) Duration.between(Instant.ofEpochMilli(Timestamp), EpochUtc).getSeconds(),
                                            buffer,
                                            offset + 24);
        EndianUtilities.stringToBytes(CreatorApp, buffer, offset + 28, 4);
        EndianUtilities.writeBytesBigEndian(CreatorVersion, buffer, offset + 32);
        EndianUtilities.stringToBytes(CreatorHostOS, buffer, offset + 36, 4);
        EndianUtilities.writeBytesBigEndian(OriginalSize, buffer, offset + 40);
        EndianUtilities.writeBytesBigEndian(CurrentSize, buffer, offset + 48);
        EndianUtilities.writeBytesBigEndian((short) Geometry.getCylinders(), buffer, offset + 56);
        buffer[offset + 58] = (byte) Geometry.getHeadsPerCylinder();
        buffer[offset + 59] = (byte) Geometry.getSectorsPerTrack();
        EndianUtilities.writeBytesBigEndian(DiskType.ordinal(), buffer, offset + 60);
        EndianUtilities.writeBytesBigEndian(Checksum, buffer, offset + 64);
        EndianUtilities.writeBytesBigEndian(UniqueId, buffer, offset + 68);
        buffer[84] = SavedState;
        Arrays.fill(buffer, 85, 85 + 427, (byte) 0);
    }
}
