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
import java.util.Arrays;
import java.util.UUID;

import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


public class DynamicHeader {

    public static final UUID EMPTY = new UUID(0, 0);

    public static final String HeaderCookie = "cxsparse";

    public static final int Version1 = 0x00010000;

    public static final int DefaultBlockSize = 0x00200000;

    public int blockSize;

    public int checksum;

    public String cookie;

    public long dataOffset;

    public int headerVersion;

    public int maxTableEntries;

    public ParentLocator[] parentLocators;

    public long parentTimestamp;

    public String parentUnicodeName;

    public UUID parentUniqueId = EMPTY;

    public long tableOffset;

    public DynamicHeader() {
    }

    public DynamicHeader(long dataOffset, long tableOffset, int blockSize, long diskSize) {
        cookie = HeaderCookie;
        this.dataOffset = dataOffset;
        this.tableOffset = tableOffset;
        headerVersion = Version1;
        this.blockSize = blockSize;
        maxTableEntries = (int) ((diskSize + blockSize - 1) / blockSize);
        parentTimestamp = Footer.EpochUtc.toEpochMilli();
        parentUnicodeName = "";
        parentLocators = new ParentLocator[8];
        for (int i = 0; i < 8; ++i) {
            parentLocators[i] = new ParentLocator();
        }
    }

    public DynamicHeader(DynamicHeader toCopy) {
        cookie = toCopy.cookie;
        dataOffset = toCopy.dataOffset;
        tableOffset = toCopy.tableOffset;
        headerVersion = toCopy.headerVersion;
        maxTableEntries = toCopy.maxTableEntries;
        blockSize = toCopy.blockSize;
        checksum = toCopy.checksum;
        parentUniqueId = toCopy.parentUniqueId;
        parentTimestamp = toCopy.parentTimestamp;
        parentUnicodeName = toCopy.parentUnicodeName;
        parentLocators = new ParentLocator[toCopy.parentLocators.length];
        for (int i = 0; i < parentLocators.length; ++i) {
            parentLocators[i] = new ParentLocator(toCopy.parentLocators[i]);
        }
    }

    public static DynamicHeader fromBytes(byte[] data, int offset) {
        DynamicHeader result = new DynamicHeader();
        result.cookie = EndianUtilities.bytesToString(data, offset, 8);
        result.dataOffset = EndianUtilities.toInt64BigEndian(data, offset + 8);
        result.tableOffset = EndianUtilities.toInt64BigEndian(data, offset + 16);
        result.headerVersion = EndianUtilities.toUInt32BigEndian(data, offset + 24);
        result.maxTableEntries = EndianUtilities.toInt32BigEndian(data, offset + 28);
        result.blockSize = EndianUtilities.toUInt32BigEndian(data, offset + 32);
        result.checksum = EndianUtilities.toUInt32BigEndian(data, offset + 36);
        result.parentUniqueId = EndianUtilities.toGuidBigEndian(data, offset + 40);
        result.parentTimestamp = Footer.EpochUtc.plusSeconds(EndianUtilities.toUInt32BigEndian(data, offset + 56))
                .toEpochMilli();
        result.parentUnicodeName = new String(data, offset + 64, 512, StandardCharsets.UTF_16BE).replaceFirst("\0*$", "");

        result.parentLocators = new ParentLocator[8];
        for (int i = 0; i < 8; ++i) {
            result.parentLocators[i] = ParentLocator.fromBytes(data, offset + 576 + i * 24);
        }

        return result;
    }

    public void toBytes(byte[] data, int offset) {
        EndianUtilities.stringToBytes(cookie, data, offset, 8);
        EndianUtilities.writeBytesBigEndian(dataOffset, data, offset + 8);
        EndianUtilities.writeBytesBigEndian(tableOffset, data, offset + 16);
        EndianUtilities.writeBytesBigEndian(headerVersion, data, offset + 24);
        EndianUtilities.writeBytesBigEndian(maxTableEntries, data, offset + 28);
        EndianUtilities.writeBytesBigEndian(blockSize, data, offset + 32);
        EndianUtilities.writeBytesBigEndian(checksum, data, offset + 36);
        EndianUtilities.writeBytesBigEndian(parentUniqueId, data, offset + 40);
        EndianUtilities.writeBytesBigEndian((int) Duration.between(Footer.EpochUtc, Instant.ofEpochMilli(parentTimestamp))
                                                    .getSeconds(),
                                            data,
                                            offset + 56);
        EndianUtilities.writeBytesBigEndian(0, data, offset + 60);
        Arrays.fill(data, offset + 64, offset + 64 + 512, (byte) 0);
        byte[] bytes = parentUnicodeName.getBytes(StandardCharsets.UTF_16BE);
        System.arraycopy(bytes, 0, data, offset + 64, bytes.length);

        for (int i = 0; i < 8; ++i) {
            parentLocators[i].toBytes(data, offset + 576 + i * 24);
        }

        Arrays.fill(data, offset + 1024 - 256, offset + 1024, (byte) 0);
    }

    public boolean isValid() {
//Debug.println(HeaderCookie.equals(Cookie) + ", " + isChecksumValid() + ", " + (headerVersion == Version1));
        return HeaderCookie.equals(cookie) && isChecksumValid() && headerVersion == Version1;
    }

    public boolean isChecksumValid() {
//Debug.println(Checksum == calculateChecksum());
        return checksum == calculateChecksum();
    }

    public int updateChecksum() {
        checksum = calculateChecksum();
        return checksum;
    }

    public static DynamicHeader fromStream(Stream stream) {
        return fromBytes(StreamUtilities.readExact(stream, 1024), 0);
    }

    private int calculateChecksum() {
        DynamicHeader copy = new DynamicHeader(this);
        copy.checksum = 0;
        byte[] asBytes = new byte[1024];
        copy.toBytes(asBytes, 0);
        int checksum = 0;
        for (byte value : asBytes) {
            checksum += value & 0xff;
        }
        checksum = ~checksum;
        return checksum;
    }
}
