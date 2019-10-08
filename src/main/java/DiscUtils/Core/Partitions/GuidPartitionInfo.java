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

package DiscUtils.Core.Partitions;

import java.util.UUID;

import DiscUtils.Core.PhysicalVolumeType;
import DiscUtils.Streams.SparseStream;


/**
 * Provides access to partition records in a GUID partition table.
 */
public final class GuidPartitionInfo extends PartitionInfo {
    private final GptEntry _entry;

    private final GuidPartitionTable _table;

    public GuidPartitionInfo(GuidPartitionTable table, GptEntry entry) {
        _table = table;
        _entry = entry;
    }

    /**
     * Gets the attributes of the partition.
     */
    public long getAttributes() {
        return _entry.Attributes;
    }

    /**
     * Always returns Zero.
     */
    public byte getBiosType() {
        return 0;
    }

    /**
     * Gets the first sector of the partion (relative to start of disk) as a
     * Logical Block Address.
     */
    public long getFirstSector() {
        return _entry.FirstUsedLogicalBlock;
    }

    /**
     * Gets the type of the partition, as a GUID.
     */
    public UUID getGuidType() {
        return _entry.PartitionType;
    }

    /**
     * Gets the unique identity of this specific partition.
     */
    public UUID getIdentity() {
        return _entry.Identity;
    }

    /**
     * Gets the last sector of the partion (relative to start of disk) as a
     * Logical Block Address (inclusive).
     */
    public long getLastSector() {
        return _entry.LastUsedLogicalBlock;
    }

    /**
     * Gets the name of the partition.
     */
    public String getName() {
        return _entry.Name;
    }

    /**
     * Gets the type of the partition as a string.
     */
    public String getTypeAsString() {
        return _entry.getFriendlyPartitionType();
    }

    public PhysicalVolumeType getVolumeType() {
        return PhysicalVolumeType.GptPartition;
    }

    /**
     * Opens a stream to access the content of the partition.
     *
     * @return The new stream.
     */
    public SparseStream open() {
        return _table.open(_entry);
    }

}
