//
// Copyright (c) 2014, Quamotion
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

package discUtils.dmg;

import java.util.UUID;

import discUtils.core.PhysicalVolumeType;
import discUtils.core.partitions.PartitionInfo;
import discUtils.streams.SparseStream;
import discUtils.streams.SubStream;


public class UdifPartitionInfo extends PartitionInfo {
    private final CompressedBlock block;

    private final Disk disk;

    public UdifPartitionInfo(Disk disk, CompressedBlock block) {
        this.block = block;
        this.disk = disk;
    }

    @Override public byte getBiosType() {
        return 0;
    }

    @Override public long getFirstSector() {
        return block.firstSector;
    }

    @Override public UUID getGuidType() {
        return new UUID(0L, 0L);
    }

    @Override public long getLastSector() {
        return block.firstSector + block.sectorCount;
    }

    @Override public long getSectorCount() {
        return block.sectorCount;
    }

    @Override public String getTypeAsString() {
        return getClass().getName();
    }

    @Override public PhysicalVolumeType getVolumeType() {
        return PhysicalVolumeType.ApplePartition;
    }

    @Override public SparseStream open() {
        return new SubStream(disk.getContent(), getFirstSector() * disk.getSectorSize(), getSectorCount() * disk.getSectorSize());
    }
}
