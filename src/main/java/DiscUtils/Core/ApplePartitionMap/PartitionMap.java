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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import DiscUtils.Core.Partitions.PartitionInfo;
import DiscUtils.Core.Partitions.PartitionTable;
import DiscUtils.Core.Partitions.WellKnownPartitionType;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * Interprets Apple Partition Map structures that partition a disk.
 */
public final class PartitionMap extends PartitionTable {
    private final List<PartitionMapEntry> _partitions;

    private final Stream _stream;

    /**
     * Initializes a new instance of the PartitionMap class.
     *
     * @param stream Stream containing the contents of a disk.
     */
    public PartitionMap(Stream stream) {
        _stream = stream;

        _stream.setPosition(0);
        byte[] initialBytes = StreamUtilities.readExact(_stream, 1024);

        BlockZero b0 = new BlockZero();
        b0.readFrom(initialBytes, 0);

        PartitionMapEntry initialPart = new PartitionMapEntry(_stream);
        initialPart.readFrom(initialBytes, 512);

        byte[] partTableData = StreamUtilities.readExact(_stream, (initialPart.MapEntries - 1) * 512);

        _partitions = new ArrayList<>(initialPart.MapEntries - 1);
        for (int i = 0; i < initialPart.MapEntries - 1; ++i) {
            PartitionMapEntry partitionMapEntry = new PartitionMapEntry(_stream);
            _partitions.add(partitionMapEntry);
            partitionMapEntry.readFrom(partTableData, 512 * i);
        }
    }

    /**
     * Gets the GUID of the disk, always returns UUID.Empty.
     */
    public UUID getDiskGuid() {
        return EMPTY;
    }

    /**
     * Gets the partitions present on the disk.
     */
    public List<PartitionInfo> getPartitions() {
        return Collections.unmodifiableList(_partitions);
    }

    /**
     * Creates a new partition that encompasses the entire disk.
     *
     * @param type The partition type.
     * @param active Whether the partition is active (bootable).
     * @return The index of the partition.The partition table must be empty
     *         before this method is called,
     *         otherwise IOException is thrown.
     */
    public int create(WellKnownPartitionType type, boolean active) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new partition with a target size.
     *
     * @param size The target size (in bytes).
     * @param type The partition type.
     * @param active Whether the partition is active (bootable).
     * @return The index of the new partition.
     */
    public int create(long size, WellKnownPartitionType type, boolean active) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new aligned partition that encompasses the entire disk.
     *
     * @param type The partition type.
     * @param active Whether the partition is active (bootable).
     * @param alignment The alignment (in byte).
     * @return The index of the partition.The partition table must be empty
     *         before this method is called,
     *         otherwise IOException is thrown.
     *         Traditionally partitions were aligned to the physical structure
     *         of the underlying disk,
     *         however with modern storage greater efficiency is acheived by
     *         aligning partitions on
     *         large values that are a power of two.
     */
    public int createAligned(WellKnownPartitionType type, boolean active, int alignment) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new aligned partition with a target size.
     *
     * @param size The target size (in bytes).
     * @param type The partition type.
     * @param active Whether the partition is active (bootable).
     * @param alignment The alignment (in byte).
     * @return The index of the new partition.
     *         Traditionally partitions were aligned to the physical structure
     *         of the underlying disk,
     *         however with modern storage greater efficiency is achieved by
     *         aligning partitions on
     *         large values that are a power of two.
     */
    public int createAligned(long size, WellKnownPartitionType type, boolean active, int alignment) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes a partition at a given index.
     *
     * @param index The index of the partition.
     */
    public void delete(int index) {
        throw new UnsupportedOperationException();
    }
}
