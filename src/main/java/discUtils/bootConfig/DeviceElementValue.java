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

import java.util.UUID;

import discUtils.core.PhysicalVolumeInfo;
import discUtils.core.PhysicalVolumeType;
import vavi.util.ByteUtil;


public class DeviceElementValue extends ElementValue {

    private static final UUID EMPTY = new UUID(0L, 0L);

    private final UUID parentObject;

    private final DeviceRecord record;

    public DeviceElementValue() {
        parentObject = EMPTY;
        PartitionRecord record = new PartitionRecord();
        record.setType(5);
        this.record = record;
    }

    public DeviceElementValue(UUID parentObject, PhysicalVolumeInfo pvi) {
        this.parentObject = parentObject;
        PartitionRecord record = new PartitionRecord();
        record.setType(6);
        if (pvi.getVolumeType() == PhysicalVolumeType.BiosPartition) {
            record.setPartitionType(1);
            record.setDiskIdentity(new byte[4]);
            ByteUtil.writeLeInt(pvi.getDiskSignature(), record.getDiskIdentity(), 0);
            record.setPartitionIdentity(new byte[8]);
            ByteUtil.writeLeLong(pvi.getPhysicalStartSector() * 512, record.getPartitionIdentity(), 0);
        } else if (pvi.getVolumeType() == PhysicalVolumeType.GptPartition) {
            record.setPartitionType(0);
            record.setDiskIdentity(new byte[16]);
            ByteUtil.writeLeUUID(pvi.getDiskIdentity(), record.getDiskIdentity(), 0);
            record.setPartitionIdentity(new byte[16]);
            ByteUtil.writeLeUUID(pvi.getPartitionIdentity(), record.getPartitionIdentity(), 0);
        } else {
            throw new IllegalArgumentException(String.format("Unknown how to convert volume type %s to a Device element",
                                                             pvi.getVolumeType()));
        }
        this.record = record;
    }

    public DeviceElementValue(byte[] value) {
        parentObject = ByteUtil.readLeUUID(value, 0x00);
        record = DeviceRecord.parse(value, 0x10);
    }

    public ElementFormat getFormat() {
        return ElementFormat.Device;
    }

    public UUID getParentObject() {
        return parentObject;
    }

    public String toString() {
        if (!parentObject.equals(EMPTY)) {
            return parentObject + ":" + record;
        }

        if (record != null) {
            return record.toString();
        }

        return "<unknown>";
    }

    public byte[] getBytes() {
        byte[] buffer = new byte[record.getSize() + 0x10];
        ByteUtil.writeLeUUID(parentObject, buffer, 0);
        record.getBytes(buffer, 0x10);
        return buffer;
    }
}
