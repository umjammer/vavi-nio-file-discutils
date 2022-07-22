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

package discUtils.core.applePartitionMap;

import java.util.UUID;

import discUtils.core.PhysicalVolumeType;
import discUtils.core.partitions.PartitionInfo;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.SparseStream;
import discUtils.streams.SubStream;
import discUtils.streams.util.EndianUtilities;
import dotnet4j.io.Stream;


public final class PartitionMapEntry extends PartitionInfo implements IByteArraySerializable {

    private final Stream diskStream;

    public int bootBlock;

    public int bootBytes;

    public int flags;

    public int logicalBlocks;

    public int logicalBlockStart;

    public int mapEntries;

    public String name;

    public int physicalBlocks;

    public int physicalBlockStart;

    public short signature;

    public String type;

    public PartitionMapEntry(Stream diskStream) {
        this.diskStream = diskStream;
    }

    public byte getBiosType() {
        return (byte) 0xAF;
    }

    public long getFirstSector() {
        return physicalBlockStart;
    }

    public UUID getGuidType() {
        return new UUID(0L, 0L);
    }

    public long getLastSector() {
        return physicalBlockStart + physicalBlocks - 1;
    }

    public String getTypeAsString() {
        return type;
    }

    public PhysicalVolumeType getVolumeType() {
        return PhysicalVolumeType.ApplePartition;
    }

    public int size() {
        return 512;
    }

    public int readFrom(byte[] buffer, int offset) {
        signature = EndianUtilities.toUInt16BigEndian(buffer, offset + 0);
        mapEntries = EndianUtilities.toUInt32BigEndian(buffer, offset + 4);
        physicalBlockStart = EndianUtilities.toUInt32BigEndian(buffer, offset + 8);
        physicalBlocks = EndianUtilities.toUInt32BigEndian(buffer, offset + 12);
        name = EndianUtilities.bytesToString(buffer, offset + 16, 32).replaceFirst("\0*$", "");
        type = EndianUtilities.bytesToString(buffer, offset + 48, 32).replaceFirst("\0*$", "");
        logicalBlockStart = EndianUtilities.toUInt32BigEndian(buffer, offset + 80);
        logicalBlocks = EndianUtilities.toUInt32BigEndian(buffer, offset + 84);
        flags = EndianUtilities.toUInt32BigEndian(buffer, offset + 88);
        bootBlock = EndianUtilities.toUInt32BigEndian(buffer, offset + 92);
        bootBytes = EndianUtilities.toUInt32BigEndian(buffer, offset + 96);

        return 512;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public SparseStream open() {
        return new SubStream(diskStream, physicalBlockStart * 512L, physicalBlocks * 512L);
    }
}
