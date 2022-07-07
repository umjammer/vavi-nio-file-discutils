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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import discUtils.core.partitions.PartitionInfo;
import discUtils.core.partitions.PartitionTable;
import discUtils.core.partitions.WellKnownPartitionType;


public class UdifPartitionTable extends PartitionTable {
    private final UdifBuffer _buffer;

    private final Disk _disk;

    private final List<PartitionInfo> _partitions;

    public UdifPartitionTable(Disk disk, UdifBuffer buffer) {
        _buffer = buffer;
        _partitions = new ArrayList<>();
        _disk = disk;
        for (CompressedBlock block : _buffer.getBlocks()) {
            UdifPartitionInfo partition = new UdifPartitionInfo(_disk, block);
            _partitions.add(partition);
        }
    }

    public UUID getDiskGuid() {
        return EMPTY;
    }

    /**
     * Gets the partitions present on the disk.
     */
    public List<PartitionInfo> getPartitions() {
        return Collections.unmodifiableList(_partitions);
    }

    public void delete(int index) {
        throw new UnsupportedOperationException();
    }

    public int createAligned(long size, WellKnownPartitionType type, boolean active, int alignment) {
        throw new UnsupportedOperationException();
    }

    public int create(long size, WellKnownPartitionType type, boolean active) {
        throw new UnsupportedOperationException();
    }

    public int createAligned(WellKnownPartitionType type, boolean active, int alignment) {
        throw new UnsupportedOperationException();
    }

    public int create(WellKnownPartitionType type, boolean active) {
        throw new UnsupportedOperationException();
    }
}
