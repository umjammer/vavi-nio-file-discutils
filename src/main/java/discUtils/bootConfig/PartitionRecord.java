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

package discUtils.bootConfig;

import java.util.Arrays;
import java.util.UUID;

import vavi.util.ByteUtil;


public class PartitionRecord extends DeviceRecord {

    private byte[] diskIdentity;

    public byte[] getDiskIdentity() {
        return diskIdentity;
    }

    public void setDiskIdentity(byte[] value) {
        diskIdentity = value;
    }

    private byte[] partitionIdentity;

    public byte[] getPartitionIdentity() {
        return partitionIdentity;
    }

    public void setPartitionIdentity(byte[] value) {
        partitionIdentity = value;
    }

    private int partitionType;

    public int getPartitionType() {
        return partitionType;
    }

    public void setPartitionType(int value) {
        partitionType = value;
    }

    @Override
    public int getSize() {
        return 0x48;
    }

    @Override
    public void getBytes(byte[] data, int offset) {
        writeHeader(data, offset);
        if (getType() == 5) {
            Arrays.fill(data, offset + 0x10, offset + 0x10 + 0x38, (byte) 0);
        } else if (getType() == 6) {
            ByteUtil.writeLeInt(getPartitionType(), data, offset + 0x24);
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
                                     ByteUtil.readLeLong(getPartitionIdentity(), 0));
            }

            UUID diskGuid = ByteUtil.readLeUUID(getDiskIdentity(), 0);
            UUID partitionGuid = ByteUtil.readLeUUID(getPartitionIdentity(), 0);
            return "(disk:%s partition:%s)".formatted(diskGuid, partitionGuid);
        }

        if (getType() == 8) {
            return "custom:<unknown>";
        }

        return "<unknown>";
    }

    @Override
    protected void doParse(byte[] data, int offset) {
        super.doParse(data, offset);
        if (getType() == 5) {
            // Nothing to do - just empty...
        } else if (getType() == 6) {
            setPartitionType(ByteUtil.readLeInt(data, offset + 0x24));
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
