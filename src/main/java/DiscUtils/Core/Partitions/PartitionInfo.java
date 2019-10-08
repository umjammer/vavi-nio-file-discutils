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
 * Base class representing a disk partition.
 * The purpose of this class is to provide a minimal view of a partition,
 * such that callers can access existing partitions without specific knowledge
 * of
 * the partitioning system.
 */
public abstract class PartitionInfo {
    /**
     * Gets the type of the partition, in legacy BIOS form, when available.
     * Zero for GUID-style partitions.
     */
    public abstract byte getBiosType();

    /**
     * Gets the first sector of the partion (relative to start of disk) as a
     * Logical Block Address.
     */
    public abstract long getFirstSector();

    /**
     * Gets the type of the partition, as a GUID, when available.
     *
     * {@link #System.Guid}
     * .Empty for MBR-style partitions.
     */
    public abstract UUID getGuidType();

    /**
     * Gets the last sector of the partion (relative to start of disk) as a
     * Logical Block Address (inclusive).
     */
    public abstract long getLastSector();

    /**
     * Gets the length of the partition in sectors.
     */
    public long getSectorCount() {
        return 1 + getLastSector() - getFirstSector();
    }

    /**
     * Gets the partition type as a 'friendly' string.
     */
    public abstract String getTypeAsString();

    /**
     * Gets the physical volume type for this type of partition.
     */
    public abstract PhysicalVolumeType getVolumeType();

    /**
     * Opens a stream that accesses the partition's contents.
     *
     * @return The new stream.
     */
    public abstract SparseStream open();

    /**
     * Gets a summary of the partition information as 'first - last (type)'.
     *
     * @return A string representation of the partition information.
     */
    public String toString() {
            return String.format("0x%x - 0x%x (%s)",
                                 getFirstSector(),
                                 getLastSector(),
                                 getTypeAsString());
    }
}
