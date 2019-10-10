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

package DiscUtils.BootConfig;

import DiscUtils.Streams.Util.EndianUtilities;


public abstract class DeviceRecord {
    private int __Length;

    public int getLength() {
        return __Length;
    }

    public void setLength(int value) {
        __Length = value;
    }

    public abstract int getSize();

    private int __Type;

    public int getType() {
        return __Type;
    }

    public void setType(int value) {
        __Type = value;
    }

    public static DeviceRecord parse(byte[] data, int offset) {
        int type = EndianUtilities.toInt32LittleEndian(data, offset);
        int length = EndianUtilities.toInt32LittleEndian(data, offset + 0x8);
        if (offset + length > data.length) {
            throw new IllegalArgumentException("Device record is truncated");
        }

        DeviceRecord newRecord = null;
        switch (type) {
        case 0:
            newRecord = new DeviceAndPathRecord();
            break;
        case 5:
        case 6:
            // Logical 'boot' device
            // Disk partition
            newRecord = new PartitionRecord();
            break;
        case 8:
            break;
        default:
            throw new IllegalArgumentException("Unknown device type: " + type);

        }
        // custom:nnnnnn
        if (newRecord != null) {
            newRecord.doParse(data, offset);
        }

        return newRecord;
    }

    public abstract void getBytes(byte[] data, int offset);

    protected void doParse(byte[] data, int offset) {
        setType(EndianUtilities.toInt32LittleEndian(data, offset));
        setLength(EndianUtilities.toInt32LittleEndian(data, offset + 0x8));
    }

    protected void writeHeader(byte[] data, int offset) {
        setLength(getSize());
        EndianUtilities.writeBytesLittleEndian(getType(), data, offset);
        EndianUtilities.writeBytesLittleEndian(getSize(), data, offset + 0x8);
    }
}
