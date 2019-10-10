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

import java.util.UUID;

import DiscUtils.Core.PhysicalVolumeInfo;
import DiscUtils.Core.PhysicalVolumeType;
import DiscUtils.Streams.Util.EndianUtilities;


public class DeviceElementValue extends ElementValue {
    private final UUID _parentObject;

    private final DeviceRecord _record;

    public DeviceElementValue() {
        _parentObject = new UUID(0, 0);
        PartitionRecord record = new PartitionRecord();
        record.setType(5);
        _record = record;
    }

    public DeviceElementValue(UUID parentObject, PhysicalVolumeInfo pvi) {
        _parentObject = parentObject;
        PartitionRecord record = new PartitionRecord();
        record.setType(6);
        if (pvi.getVolumeType() == PhysicalVolumeType.BiosPartition) {
            record.setPartitionType(1);
            record.setDiskIdentity(new byte[4]);
            EndianUtilities.writeBytesLittleEndian(pvi.getDiskSignature(), record.getDiskIdentity(), 0);
            record.setPartitionIdentity(new byte[8]);
            EndianUtilities.writeBytesLittleEndian(pvi.getPhysicalStartSector() * 512, record.getPartitionIdentity(), 0);
        } else if (pvi.getVolumeType() == PhysicalVolumeType.GptPartition) {
            record.setPartitionType(0);
            record.setDiskIdentity(new byte[16]);
            EndianUtilities.writeBytesLittleEndian(pvi.getDiskIdentity(), record.getDiskIdentity(), 0);
            record.setPartitionIdentity(new byte[16]);
            EndianUtilities.writeBytesLittleEndian(pvi.getPartitionIdentity(), record.getPartitionIdentity(), 0);
        } else {
            throw new IllegalArgumentException(String.format("Unknown how to convert volume type {0} to a Device element",
                                                            pvi.getVolumeType()));
        }
        _record = record;
    }

    public DeviceElementValue(byte[] value) {
        _parentObject = EndianUtilities.toGuidLittleEndian(value, 0x00);
        _record = DeviceRecord.parse(value, 0x10);
    }

    public ElementFormat getFormat() {
        return ElementFormat.Device;
    }

    public UUID getParentObject() {
        return _parentObject;
    }

    public String toString() {
        try {
            if (_parentObject != new UUID(0, 0)) {
                return _parentObject + ":" + _record;
            }

            if (_record != null) {
                return _record.toString();
            }

            return "<unknown>";
        } catch (RuntimeException __dummyCatchVar0) {
            throw __dummyCatchVar0;
        } catch (Exception __dummyCatchVar0) {
            throw new RuntimeException(__dummyCatchVar0);
        }

    }

    public byte[] getBytes() {
        byte[] buffer = new byte[_record.getSize() + 0x10];
        EndianUtilities.writeBytesLittleEndian(_parentObject, buffer, 0);
        _record.getBytes(buffer, 0x10);
        return buffer;
    }

}
