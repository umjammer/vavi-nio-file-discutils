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

package DiscUtils.Core.ApplePartitionMap;

import java.util.UUID;

import DiscUtils.Core.PhysicalVolumeType;
import DiscUtils.Core.Partitions.PartitionInfo;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Util.EndianUtilities;
import dotnet4j.io.Stream;


public final class PartitionMapEntry extends PartitionInfo implements IByteArraySerializable {
    private final Stream _diskStream;

    public int BootBlock;

    public int BootBytes;

    public int Flags;

    public int LogicalBlocks;

    public int LogicalBlockStart;

    public int MapEntries;

    public String Name;

    public int PhysicalBlocks;

    public int PhysicalBlockStart;

    public short Signature;

    public String Type;

    public PartitionMapEntry(Stream diskStream) {
        _diskStream = diskStream;
    }

    public byte getBiosType() {
        return (byte) 0xAF;
    }

    public long getFirstSector() {
        return PhysicalBlockStart;
    }

    public UUID getGuidType() {
        return new UUID(0L, 0L);
    }

    public long getLastSector() {
        return PhysicalBlockStart + PhysicalBlocks - 1;
    }

    public String getTypeAsString() {
        return Type;
    }

    public PhysicalVolumeType getVolumeType() {
        return PhysicalVolumeType.ApplePartition;
    }

    public int size() {
        return 512;
    }

    public int readFrom(byte[] buffer, int offset) {
        Signature = EndianUtilities.toUInt16BigEndian(buffer, offset + 0);
        MapEntries = EndianUtilities.toUInt32BigEndian(buffer, offset + 4);
        PhysicalBlockStart = EndianUtilities.toUInt32BigEndian(buffer, offset + 8);
        PhysicalBlocks = EndianUtilities.toUInt32BigEndian(buffer, offset + 12);
        Name = EndianUtilities.bytesToString(buffer, offset + 16, 32).replaceFirst("\0*$", "");
        Type = EndianUtilities.bytesToString(buffer, offset + 48, 32).replaceFirst("\0*$", "");
        LogicalBlockStart = EndianUtilities.toUInt32BigEndian(buffer, offset + 80);
        LogicalBlocks = EndianUtilities.toUInt32BigEndian(buffer, offset + 84);
        Flags = EndianUtilities.toUInt32BigEndian(buffer, offset + 88);
        BootBlock = EndianUtilities.toUInt32BigEndian(buffer, offset + 92);
        BootBytes = EndianUtilities.toUInt32BigEndian(buffer, offset + 96);
        return 512;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public SparseStream open() {
        return new SubStream(_diskStream, PhysicalBlockStart * 512, PhysicalBlocks * 512);
    }
}
