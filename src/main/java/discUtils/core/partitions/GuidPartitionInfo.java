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

package discUtils.core.partitions;

import java.util.UUID;

import discUtils.core.PhysicalVolumeType;
import discUtils.streams.SparseStream;


/**
 * Provides access to partition records in a GUID partition table.
 */
public final class GuidPartitionInfo extends PartitionInfo {

    private final GptEntry entry;

    private final GuidPartitionTable table;

    public GuidPartitionInfo(GuidPartitionTable table, GptEntry entry) {
        this.table = table;
        this.entry = entry;
    }

    /**
     * Gets the attributes of the partition.
     */
    public long getAttributes() {
        return entry.attributes;
    }

    /**
     * Always returns Zero.
     */
    @Override public byte getBiosType() {
        return 0;
    }

    /**
     * Gets the first sector of the partion (relative to start of disk) as a
     * Logical block Address.
     */
    @Override public long getFirstSector() {
        return entry.firstUsedLogicalBlock;
    }

    /**
     * Gets the type of the partition, as a GUID.
     */
    @Override public UUID getGuidType() {
        return entry.partitionType;
    }

    /**
     * Gets the unique identity of this specific partition.
     */
    public UUID getIdentity() {
        return entry.identity;
    }

    /**
     * Gets the last sector of the partion (relative to start of disk) as a
     * Logical block Address (inclusive).
     */
    @Override public long getLastSector() {
        return entry.lastUsedLogicalBlock;
    }

    /**
     * Gets the name of the partition.
     */
    public String getName() {
        return entry.name;
    }

    /**
     * Gets the type of the partition as a string.
     */
    @Override public String getTypeAsString() {
        return entry.getFriendlyPartitionType();
    }

    @Override public PhysicalVolumeType getVolumeType() {
        return PhysicalVolumeType.GptPartition;
    }

    /**
     * Opens a stream to access the content of the partition.
     *
     * @return The new stream.
     */
    @Override public SparseStream open() {
        return table.open(entry);
    }
}
