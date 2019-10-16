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

import java.util.Arrays;
import java.util.UUID;

import DiscUtils.Streams.Util.EndianUtilities;


public class PartitionRecord extends DeviceRecord {
    private byte[] __DiskIdentity;

    public byte[] getDiskIdentity() {
        return __DiskIdentity;
    }

    public void setDiskIdentity(byte[] value) {
        __DiskIdentity = value;
    }

    private byte[] __PartitionIdentity;

    public byte[] getPartitionIdentity() {
        return __PartitionIdentity;
    }

    public void setPartitionIdentity(byte[] value) {
        __PartitionIdentity = value;
    }

    private int __PartitionType;

    public int getPartitionType() {
        return __PartitionType;
    }

    public void setPartitionType(int value) {
        __PartitionType = value;
    }

    public int getSize() {
        return 0x48;
    }

    public void getBytes(byte[] data, int offset) {
        writeHeader(data, offset);
        if (getType() == 5) {
            Arrays.fill(data, offset + 0x10, offset + 0x10 + 0x38, (byte) 0);
        } else if (getType() == 6) {
            EndianUtilities.writeBytesLittleEndian(getPartitionType(), data, offset + 0x24);
            if (getPartitionType() == 1) {
                System.arraycopy(getDiskIdentity(), 0, data, offset + 0x28, 4);
                System.arraycopy(getPartitionIdentity(), 0, data, offset + 0x10, 8);
            } else if (getPartitionType() == 0) {
                System.arraycopy(getDiskIdentity(), 0, data, offset + 0x28, 16);
                System.arraycopy(getPartitionIdentity(), 0, data, offset + 0x10, 16);
            } else {
                throw new IllegalArgumentException("Unknown partition type: " + getPartitionType());
            }
        } else {
            throw new IllegalArgumentException("Unknown device type: " + getType());
        }
    }

    public String toString() {
        if (getType() == 5) {
            return "<boot device>";
        }

        if (getType() == 6) {
            if (getPartitionType() == 1) {
                return String.format("(disk:%02x%02x%02x%02x part-offset:%d)",
                                     getDiskIdentity()[0],
                                     getDiskIdentity()[1],
                                     getDiskIdentity()[2],
                                     getDiskIdentity()[3],
                                     EndianUtilities.toUInt64LittleEndian(getPartitionIdentity(), 0));
            }

            UUID diskGuid = EndianUtilities.toGuidLittleEndian(getDiskIdentity(), 0);
            UUID partitionGuid = EndianUtilities.toGuidLittleEndian(getPartitionIdentity(), 0);
            return String.format("(disk:%s partition:%d)", diskGuid, partitionGuid);
        }

        if (getType() == 8) {
            return "custom:<unknown>";
        }

        return "<unknown>";
    }

    protected void doParse(byte[] data, int offset) {
        super.doParse(data, offset);
        if (getType() == 5) {
        } else // Nothing to do - just empty...
        if (getType() == 6) {
            setPartitionType(EndianUtilities.toInt32LittleEndian(data, offset + 0x24));
            if (getPartitionType() == 1) {
                // BIOS disk
                setDiskIdentity(new byte[4]);
                System.arraycopy(data, offset + 0x28, getDiskIdentity(), 0, 4);
                setPartitionIdentity(new byte[8]);
                System.arraycopy(data, offset + 0x10, getPartitionIdentity(), 0, 8);
            } else if (getPartitionType() == 0) {
                // GPT disk
                setDiskIdentity(new byte[16]);
                System.arraycopy(data, offset + 0x28, getDiskIdentity(), 0, 16);
                setPartitionIdentity(new byte[16]);
                System.arraycopy(data, offset + 0x10, getPartitionIdentity(), 0, 16);
            } else {
                throw new IllegalArgumentException("Unknown partition type: " + getPartitionType());
            }
        } else {
            throw new IllegalArgumentException("Unknown device type: " + getType());
        }
    }
}
