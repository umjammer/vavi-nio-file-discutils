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

import discUtils.core.ChsAddress;
import discUtils.core.PhysicalVolumeType;
import discUtils.streams.SparseStream;


/**
 * Provides access to partition records in a BIOS (MBR) partition table.
 */
public final class BiosPartitionInfo extends PartitionInfo {

    private final BiosPartitionRecord record;

    private final BiosPartitionTable table;

    public BiosPartitionInfo(BiosPartitionTable table, BiosPartitionRecord record) {
        this.table = table;
        this.record = record;
    }

    /**
     * Gets the type of the partition.
     */
    public byte getBiosType() {
        return record.getPartitionType();
    }

    /**
     * Gets the end (inclusive) of the partition as a CHS address.
     */
    public ChsAddress getEnd() {
        return new ChsAddress(record.getEndCylinder(), record.getEndHead(), record.getEndSector());
    }

    /**
     * Gets the first sector of the partion (relative to start of disk) as a
     * Logical block Address.
     */
    public long getFirstSector() {
        return record.getLBAStartAbsolute();
    }

    /**
     * Always returns {@link UUID} empty.
     */
    public UUID getGuidType() {
        return new UUID(0L, 0L);
    }

    /**
     * Gets a value indicating whether this partition is active (bootable).
     */
    public boolean isActive() {
        return record.getStatus() != 0;
    }

    /**
     * Gets a value indicating whether the partition is a primary (rather than
     * extended) partition.
     */
    public boolean isPrimary() {
        return getPrimaryIndex() >= 0;
    }

    /**
     * Gets the last sector of the partion (relative to start of disk) as a
     * Logical block Address (inclusive).
     */
    public long getLastSector() {
        return record.getLBAStartAbsolute() + record.getLBALength() - 1;
    }

    /**
     * Gets the index of the partition in the primary partition table, or
     * {@code -1} if not a primary partition.
     */
    public int getPrimaryIndex() {
        return record.getIndex();
    }

    /**
     * Gets the start of the partition as a CHS address.
     */
    public ChsAddress getStart() {
        return new ChsAddress(record.getStartCylinder(), record.getStartHead(), record.getStartSector());
    }

    /**
     * Gets the type of the partition as a string.
     */
    public String getTypeAsString() {
        return record.getFriendlyPartitionType();
    }

    public PhysicalVolumeType getVolumeType() {
        return PhysicalVolumeType.BiosPartition;
    }

    /**
     * Opens a stream to access the content of the partition.
     *
     * @return The new stream.
     */
    public SparseStream open() {
        return table.open(record);
    }
}
