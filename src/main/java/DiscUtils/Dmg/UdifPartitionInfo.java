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

package DiscUtils.Dmg;

import java.util.UUID;

import DiscUtils.Core.PhysicalVolumeType;
import DiscUtils.Core.Partitions.PartitionInfo;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.SubStream;


public class UdifPartitionInfo extends PartitionInfo {
    private final CompressedBlock _block;

    private final Disk _disk;

    public UdifPartitionInfo(Disk disk, CompressedBlock block) {
        _block = block;
        _disk = disk;
    }

    public byte getBiosType() {
        return 0;
    }

    public long getFirstSector() {
        return _block.FirstSector;
    }

    public UUID getGuidType() {
        return new UUID(0L, 0L);
    }

    public long getLastSector() {
        return _block.FirstSector + _block.SectorCount;
    }

    public long getSectorCount() {
        return _block.SectorCount;
    }

    public String getTypeAsString() {
        return getClass().getName();
    }

    public PhysicalVolumeType getVolumeType() {
        return PhysicalVolumeType.ApplePartition;
    }

    public SparseStream open() {
        return new SubStream(_disk.getContent(), getFirstSector() * _disk.getSectorSize(), getSectorCount() * _disk.getSectorSize());
    }
}
