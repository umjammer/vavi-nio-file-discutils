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

package discUtils.core.logicalDiskManager;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import vavi.util.ByteUtil;


public abstract class DatabaseRecord {

    public int counter;

    public int dataLength;

    public int flags;

    public long id;

    public int label;

    public String name;

    public RecordType recordType;

    public String signature;

    /** VBLK */
    public int valid;

    public static DatabaseRecord readFrom(byte[] buffer, int[] offset) {
        DatabaseRecord result = null;
        if (ByteUtil.readBeInt(buffer, offset[0] + 0xC) != 0) {
            result = switch (RecordType.values()[buffer[offset[0] + 0x13] & 0xF]) {
                case Volume -> new VolumeRecord();
                case Component -> new ComponentRecord();
                case Extent -> new ExtentRecord();
                case Disk -> new DiskRecord();
                case DiskGroup -> new DiskGroupRecord();
                default -> throw new IllegalArgumentException("Unrecognized record type: " + buffer[offset[0] + 0x13]);
            };
            result.doReadFrom(buffer, offset[0]);
        }

        return result;
    }

    /**
     * @param offset {@cs out}
     */
    protected static long readVarULong(byte[] buffer, int[] offset) {
        int length = buffer[offset[0]];
        long result = 0;
        for (int i = 0; i < length; ++i) {
            result = (result << 8) | (buffer[offset[0] + i + 1] & 0xff);
        }
        offset[0] += length + 1;
        return result;
    }

    protected static long readVarLong(byte[] buffer, int[] offset) {
        return readVarULong(buffer, offset);
    }

    protected static String readVarString(byte[] buffer, int[] offset) {
        int length = buffer[offset[0]];
        String result = new String(buffer, offset[0] + 1, length, StandardCharsets.US_ASCII);
        offset[0] += length + 1;
        return result;
    }

    protected static byte readByte(byte[] buffer, int[] offset) {
        return buffer[offset[0]++];
    }

    protected static int readUInt(byte[] buffer, int[] offset) {
        offset[0] += 4;
        return ByteUtil.readBeInt(buffer, offset[0] - 4);
    }

    protected static long readLong(byte[] buffer, int[] offset) {
        offset[0] += 8;
        return ByteUtil.readBeLong(buffer, offset[0] - 8);
    }

    protected static long readULong(byte[] buffer, int[] offset) {
        offset[0] += 8;
        return ByteUtil.readBeLong(buffer, offset[0] - 8);
    }

    protected static String readString(byte[] buffer, int len, int[] offset) {
        offset[0] += len;
        return new String(buffer, offset[0] - len, len, StandardCharsets.US_ASCII);
    }

    protected static UUID readBinaryGuid(byte[] buffer, int[] offset) {
        offset[0] += 16;
        return ByteUtil.readBeUUID(buffer, offset[0] - 16);
    }

    protected void doReadFrom(byte[] buffer, int offset) {
        signature = new String(buffer, offset + 0x00, 4, StandardCharsets.US_ASCII);
        label = ByteUtil.readBeInt(buffer, offset + 0x04);
        counter = ByteUtil.readBeInt(buffer, offset + 0x08);
        valid = ByteUtil.readBeInt(buffer, offset + 0x0C);
        flags = ByteUtil.readBeInt(buffer, offset + 0x10);
        recordType = RecordType.values()[flags & 0xF];
        dataLength = ByteUtil.readBeInt(buffer, 0x14);
    }
}
