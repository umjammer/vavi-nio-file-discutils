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

package discUtils.vhd;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import discUtils.core.Geometry;
import discUtils.streams.util.EndianUtilities;
import vavi.util.ByteUtil;


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

    public int checksum;

    public String cookie;

    public String creatorApp;

    public String creatorHostOS;

    public int creatorVersion;

    public long currentSize;

    public long dataOffset;

    public FileType diskType = FileType.None;

    public int features;

    public int fileFormatVersion;

    public Geometry geometry;

    public long originalSize;

    public byte savedState;

    public long timestamp;

    public UUID uniqueId;

    public Footer(Geometry geometry, long capacity, FileType type) {
        cookie = FileCookie;
        features = FeatureReservedMustBeSet;
        fileFormatVersion = Version1;
        dataOffset = -1;
        timestamp = System.currentTimeMillis();
        creatorApp = "dutl";
        creatorVersion = Version6Point1;
        creatorHostOS = WindowsHostOS;
        originalSize = capacity;
        currentSize = capacity;
        this.geometry = geometry;
        diskType = type;
        uniqueId = UUID.randomUUID();
    }

    /**
     * /savedState = 0;
     */
    public Footer(Footer toCopy) {
        cookie = toCopy.cookie;
        features = toCopy.features;
        fileFormatVersion = toCopy.fileFormatVersion;
        dataOffset = toCopy.dataOffset;
        timestamp = toCopy.timestamp;
        creatorApp = toCopy.creatorApp;
        creatorVersion = toCopy.creatorVersion;
        creatorHostOS = toCopy.creatorHostOS;
        originalSize = toCopy.originalSize;
        currentSize = toCopy.currentSize;
        geometry = toCopy.geometry;
        diskType = toCopy.diskType;
        checksum = toCopy.checksum;
        uniqueId = toCopy.uniqueId;
        savedState = toCopy.savedState;
    }

    private Footer() {
    }

    public boolean isValid() {
//logger.log(Level.DEBUG, FileCookie.equals(Cookie) + ", " + isChecksumValid() + ", " + (fileFormatVersion == Version1));
        return FileCookie.equals(cookie) && isChecksumValid()
        // && ((features & FeatureReservedMustBeSet) != 0)
               && fileFormatVersion == Version1;
    }

    public boolean isChecksumValid() {
//logger.log(Level.DEBUG, "%x, %x\n".formatted(Checksum, calculateChecksum()));
        return checksum == calculateChecksum();
    }

    public int updateChecksum() {
        checksum = calculateChecksum();
        return checksum;
    }

    private int calculateChecksum() {
        Footer copy = new Footer(this);
        copy.checksum = 0;
        byte[] asBytes = new byte[512];
        copy.toBytes(asBytes, 0);
        int checksum = 0;
        for (byte value : asBytes) {
            checksum += value & 0xff;
        }
        checksum = ~checksum;
        return checksum;
    }

    public static Footer fromBytes(byte[] buffer, int offset) {
        Footer result = new Footer();
        result.cookie = new String(buffer, offset + 0, 8, StandardCharsets.US_ASCII);
        result.features = ByteUtil.readBeInt(buffer, offset + 8);
        result.fileFormatVersion = ByteUtil.readBeInt(buffer, offset + 12);
        result.dataOffset = ByteUtil.readBeLong(buffer, offset + 16);
        result.timestamp = EpochUtc.plusSeconds(ByteUtil.readBeInt(buffer, offset + 24)).toEpochMilli();
        result.creatorApp = new String(buffer, offset + 28, 4, StandardCharsets.US_ASCII);
        result.creatorVersion = ByteUtil.readBeInt(buffer, offset + 32);
        result.creatorHostOS = new String(buffer, offset + 36, 4, StandardCharsets.US_ASCII);
        result.originalSize = ByteUtil.readBeLong(buffer, offset + 40);
        result.currentSize = ByteUtil.readBeLong(buffer, offset + 48);
        result.geometry = new Geometry(ByteUtil.readBeShort(buffer, offset + 56) & 0xffff,
                                       buffer[58] & 0xff,
                                       buffer[59] & 0xff);
        result.diskType = FileType.valueOf(ByteUtil.readBeInt(buffer, offset + 60));
        result.checksum = ByteUtil.readBeInt(buffer, offset + 64);
        result.uniqueId = ByteUtil.readBeUUID(buffer, offset + 68);
        result.savedState = buffer[84];

        return result;
    }

    public void toBytes(byte[] buffer, int offset) {
        EndianUtilities.stringToBytes(cookie, buffer, offset + 0, 8);
        ByteUtil.writeBeInt(features, buffer, offset + 8);
        ByteUtil.writeBeInt(fileFormatVersion, buffer, offset + 12);
        ByteUtil.writeBeLong(dataOffset, buffer, offset + 16);
        ByteUtil.writeBeInt((int) Duration.between(EpochUtc, Instant.ofEpochMilli(timestamp)).getSeconds(),
                                            buffer,
                                            offset + 24);
        EndianUtilities.stringToBytes(creatorApp, buffer, offset + 28, 4);
        ByteUtil.writeBeInt(creatorVersion, buffer, offset + 32);
        EndianUtilities.stringToBytes(creatorHostOS, buffer, offset + 36, 4);
        ByteUtil.writeBeLong(originalSize, buffer, offset + 40);
        ByteUtil.writeBeLong(currentSize, buffer, offset + 48);
        ByteUtil.writeBeShort((short) geometry.getCylinders(), buffer, offset + 56);
        buffer[offset + 58] = (byte) geometry.getHeadsPerCylinder();
        buffer[offset + 59] = (byte) geometry.getSectorsPerTrack();
        ByteUtil.writeBeInt(diskType.ordinal(), buffer, offset + 60);
        ByteUtil.writeBeInt(checksum, buffer, offset + 64);
        ByteUtil.writeBeUUID(uniqueId, buffer, offset + 68);
        buffer[84] = savedState;
        Arrays.fill(buffer, 85, 85 + 427, (byte) 0);
//logger.log(Level.DEBUG, "\n" + StringUtil.getDump(buffer));
    }
}
