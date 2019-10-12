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

package DiscUtils.Core.LogicalDiskManager;

import java.util.UUID;

import DiscUtils.Streams.Util.EndianUtilities;


public abstract class DatabaseRecord {
    public int Counter;

    public int DataLength;

    public int Flags;

    public long Id;

    public int Label;

    public String Name;

    public RecordType _RecordType;

    public String Signature;

    // VBLK
    public int Valid;

    public static DatabaseRecord readFrom(byte[] buffer, int[] offset) {
        DatabaseRecord result = null;
        if (EndianUtilities.toInt32BigEndian(buffer, offset[0] + 0xC) != 0) {
            switch (RecordType.valueOf(buffer[offset[0] + 0x13] & 0xF)) {
            case Volume:
                result = new VolumeRecord();
                break;
            case Component:
                result = new ComponentRecord();
                break;
            case Extent:
                result = new ExtentRecord();
                break;
            case Disk:
                result = new DiskRecord();
                break;
            case DiskGroup:
                result = new DiskGroupRecord();
                break;
            default:
                throw new IllegalArgumentException("Unrecognized record type: " + buffer[offset[0] + 0x13]);

            }
            result.doReadFrom(buffer, offset[0]);
        }

        return result;
    }

    protected static long readVarULong(byte[] buffer, int[] offset) {
        int length = buffer[offset[0]];
        long result = 0;
        for (int i = 0; i < length; ++i) {
            result = (result << 8) | buffer[offset[0] + i + 1];
        }
        offset[0] += length + 1;
        return result;
    }

    protected static long readVarLong(byte[] buffer, int[] offset) {
        return readVarULong(buffer, offset);
    }

    protected static String readVarString(byte[] buffer, int[] offset) {
        int length = buffer[offset[0]];
        String result = EndianUtilities.bytesToString(buffer, offset[0] + 1, length);
        offset[0] += length + 1;
        return result;
    }

    protected static byte readByte(byte[] buffer, int[] offset) {
        return buffer[offset[0]++];
    }

    protected static int readUInt(byte[] buffer, int[] offset) {
        offset[0] += 4;
        return EndianUtilities.toUInt32BigEndian(buffer, offset[0] - 4);
    }

    protected static long readLong(byte[] buffer, int[] offset) {
        offset[0] += 8;
        return EndianUtilities.toInt64BigEndian(buffer, offset[0] - 8);
    }

    protected static long readULong(byte[] buffer, int[] offset) {
        offset[0] += 8;
        return EndianUtilities.toUInt64BigEndian(buffer, offset[0] - 8);
    }

    protected static String readString(byte[] buffer, int len, int[] offset) {
        offset[0] += len;
        return EndianUtilities.bytesToString(buffer, offset[0] - len, len);
    }

    protected static UUID readBinaryGuid(byte[] buffer, int[] offset) {
        offset[0] += 16;
        return EndianUtilities.toGuidBigEndian(buffer, offset[0] - 16);
    }

    protected void doReadFrom(byte[] buffer, int offset) {
        Signature = EndianUtilities.bytesToString(buffer, offset + 0x00, 4);
        Label = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x04);
        Counter = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x08);
        Valid = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x0C);
        Flags = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x10);
        _RecordType = RecordType.valueOf(Flags & 0xF);
        DataLength = EndianUtilities.toUInt32BigEndian(buffer, 0x14);
    }
}
