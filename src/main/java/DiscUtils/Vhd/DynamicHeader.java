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

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.Stream;


public class DynamicHeader {
    public static final String HeaderCookie = "cxsparse";

    public static final int Version1 = 0x00010000;

    public static final int DefaultBlockSize = 0x00200000;

    public int BlockSize;

    public int Checksum;

    public String Cookie;

    public long DataOffset;

    public int HeaderVersion;

    public int MaxTableEntries;

    public ParentLocator[] ParentLocators;

    public long ParentTimestamp;

    public String ParentUnicodeName;

    public UUID ParentUniqueId;

    public long TableOffset;

    public DynamicHeader() {
    }

    public DynamicHeader(long dataOffset, long tableOffset, int blockSize, long diskSize) {
        Cookie = HeaderCookie;
        DataOffset = dataOffset;
        TableOffset = tableOffset;
        HeaderVersion = Version1;
        BlockSize = blockSize;
        MaxTableEntries = (int) ((diskSize + blockSize - 1) / blockSize);
        ParentTimestamp = Footer.EpochUtc.toEpochMilli();
        ParentUnicodeName = "";
        ParentLocators = new ParentLocator[8];
        ParentUniqueId = new UUID(0, 0); // TODO no initializer
        for (int i = 0; i < 8; ++i) {
            ParentLocators[i] = new ParentLocator();
        }
    }

    public DynamicHeader(DynamicHeader toCopy) {
        Cookie = toCopy.Cookie;
        DataOffset = toCopy.DataOffset;
        TableOffset = toCopy.TableOffset;
        HeaderVersion = toCopy.HeaderVersion;
        MaxTableEntries = toCopy.MaxTableEntries;
        BlockSize = toCopy.BlockSize;
        Checksum = toCopy.Checksum;
        ParentUniqueId = toCopy.ParentUniqueId;
        ParentTimestamp = toCopy.ParentTimestamp;
        ParentUnicodeName = toCopy.ParentUnicodeName;
        ParentLocators = new ParentLocator[toCopy.ParentLocators.length];
        for (int i = 0; i < ParentLocators.length; ++i) {
            ParentLocators[i] = new ParentLocator(toCopy.ParentLocators[i]);
        }
    }

    public static DynamicHeader fromBytes(byte[] data, int offset) {
        DynamicHeader result = new DynamicHeader();
        result.Cookie = EndianUtilities.bytesToString(data, offset, 8);
        result.DataOffset = EndianUtilities.toInt64BigEndian(data, offset + 8);
        result.TableOffset = EndianUtilities.toInt64BigEndian(data, offset + 16);
        result.HeaderVersion = EndianUtilities.toUInt32BigEndian(data, offset + 24);
        result.MaxTableEntries = EndianUtilities.toInt32BigEndian(data, offset + 28);
        result.BlockSize = EndianUtilities.toUInt32BigEndian(data, offset + 32);
        result.Checksum = EndianUtilities.toUInt32BigEndian(data, offset + 36);
        result.ParentUniqueId = EndianUtilities.toGuidBigEndian(data, offset + 40);
        result.ParentTimestamp = Instant.EPOCH.plusSeconds(EndianUtilities.toUInt32BigEndian(data, offset + 56)).toEpochMilli();
        result.ParentUnicodeName = new String(data, offset + 64, 512, Charset.forName("UTF-16BE")).replaceFirst("\0*$", "");
        result.ParentLocators = new ParentLocator[8];
        for (int i = 0; i < 8; ++i) {
            result.ParentLocators[i] = ParentLocator.fromBytes(data, offset + 576 + i * 24);
        }
        return result;
    }

    public void toBytes(byte[] data, int offset) {
        EndianUtilities.stringToBytes(Cookie, data, offset, 8);
        EndianUtilities.writeBytesBigEndian(DataOffset, data, offset + 8);
        EndianUtilities.writeBytesBigEndian(TableOffset, data, offset + 16);
        EndianUtilities.writeBytesBigEndian(HeaderVersion, data, offset + 24);
        EndianUtilities.writeBytesBigEndian(MaxTableEntries, data, offset + 28);
        EndianUtilities.writeBytesBigEndian(BlockSize, data, offset + 32);
        EndianUtilities.writeBytesBigEndian(Checksum, data, offset + 36);
        EndianUtilities.writeBytesBigEndian(ParentUniqueId, data, offset + 40);
        EndianUtilities.writeBytesBigEndian((int) Instant.ofEpochMilli(ParentTimestamp).getEpochSecond(), data, offset + 56);
        EndianUtilities.writeBytesBigEndian(0, data, offset + 60);
        Arrays.fill(data, offset + 64, offset + 64 + 512, (byte) 0);
        byte[] bytes = ParentUnicodeName.getBytes(Charset.forName("UTF-16BE"));
        System.arraycopy(bytes, 0, data, offset + 64, bytes.length);
        for (int i = 0; i < 8; ++i) {
            ParentLocators[i].toBytes(data, offset + 576 + i * 24);
        }
        Arrays.fill(data, offset + 1024 - 256, offset + 1024, (byte) 0);
    }

    public boolean isValid() {
//Debug.println(HeaderCookie.equals(Cookie) + ", " + isChecksumValid() + ", " + (HeaderVersion == Version1));
        return HeaderCookie.equals(Cookie) && isChecksumValid() && HeaderVersion == Version1;
    }

    public boolean isChecksumValid() {
        return Checksum == calculateChecksum();
    }

    public int updateChecksum() {
        Checksum = calculateChecksum();
        return Checksum;
    }

    public static DynamicHeader fromStream(Stream stream) {
        return fromBytes(StreamUtilities.readExact(stream, 1024), 0);
    }

    private int calculateChecksum() {
        DynamicHeader copy = new DynamicHeader(this);
        copy.Checksum = 0;
        byte[] asBytes = new byte[1024];
        copy.toBytes(asBytes, 0);
        int checksum = 0;
        for (int value : asBytes) {
            checksum += value & 0xff;
        }
        checksum = ~checksum;
        return checksum;
    }
}
