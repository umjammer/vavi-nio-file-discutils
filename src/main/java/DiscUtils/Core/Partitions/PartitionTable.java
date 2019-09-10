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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VolumeManager;
import DiscUtils.Core.CoreCompat.ReflectionHelper;
import DiscUtils.Core.Raw.Disk;
import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Base class for classes which represent a disk partitioning scheme.
 * After modifying the table, by creating or deleting a partition assume that
 * any
 * previously stored partition indexes of higher value are no longer valid.
 * Re-enumerate
 * the partitions to discover the next index-to-partition mapping.
 */
public abstract class PartitionTable {
    private static List<PartitionTableFactory> _factories;

    /**
     * Gets the number of User partitions on the disk.
     */
    public int getCount() {
        return getPartitions().size();
    }

    /**
     * Gets the GUID that uniquely identifies this disk, if supported (else
     * returns
     * {@code null}
     * ).
     */
    public abstract UUID getDiskGuid();

    private static List<PartitionTableFactory> getFactories() {
        try {
            if (_factories == null) {
                List<PartitionTableFactory> factories = new ArrayList<>();
                for (Class<?> type : ReflectionHelper.getAssembly(VolumeManager.class)) {
                    for (PartitionTableFactoryAttribute attr : ReflectionHelper
                            .getCustomAttributes(type, PartitionTableFactoryAttribute.class, false)) {
                        factories.add((PartitionTableFactory) type.newInstance());
                    }
                }
                _factories = factories;
            }

            return _factories;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Gets information about a particular User partition.
     * 
     * @param index The index of the partition.
     * @return Information about the partition.
     */
    public PartitionInfo get___idx(int index) {
        return getPartitions().get(index);
    }

    /**
     * Gets the list of partitions that contain user data (i.e. non-system /
     * empty).
     */
    public abstract List<PartitionInfo> getPartitions();

    /**
     * Determines if a disk is partitioned with a known partitioning scheme.
     * 
     * @param content The content of the disk to check.
     * @return
     *         {@code true}
     *         if the disk is partitioned, else
     *         {@code false}
     *         .
     */
    public static boolean isPartitioned(Stream content) {
        for (Object __dummyForeachVar2 : getFactories()) {
            PartitionTableFactory partTableFactory = (PartitionTableFactory) __dummyForeachVar2;
            if (partTableFactory.detectIsPartitioned(content)) {
                return true;
            }

        }
        return false;
    }

    /**
     * Determines if a disk is partitioned with a known partitioning scheme.
     * 
     * @param disk The disk to check.
     * @return
     *         {@code true}
     *         if the disk is partitioned, else
     *         {@code false}
     *         .
     */
    public static boolean isPartitioned(VirtualDisk disk) throws IOException {
        return isPartitioned(disk.getContent());
    }

    /**
     * Gets all of the partition tables found on a disk.
     * 
     * @param disk The disk to inspect.
     * @return It is rare for a disk to have multiple partition tables, but
     *         theoretically
     *         possible.
     */
    public static List<PartitionTable> getPartitionTables(VirtualDisk disk) throws IOException {
        List<PartitionTable> tables = new ArrayList<>();
        for (Object __dummyForeachVar3 : getFactories()) {
            PartitionTableFactory factory = (PartitionTableFactory) __dummyForeachVar3;
            PartitionTable table = factory.detectPartitionTable(disk);
            if (table != null) {
                tables.add(table);
            }

        }
        return tables;
    }

    /**
     * Gets all of the partition tables found on a disk.
     * 
     * @param contentStream The content of the disk to inspect.
     * @return It is rare for a disk to have multiple partition tables, but
     *         theoretically
     *         possible.
     */
    public static List<PartitionTable> getPartitionTables(Stream contentStream) throws IOException {
        return getPartitionTables(new Disk(contentStream, Ownership.None));
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
    public abstract int create(WellKnownPartitionType type, boolean active);

    /**
     * Creates a new partition with a target size.
     * 
     * @param size The target size (in bytes).
     * @param type The partition type.
     * @param active Whether the partition is active (bootable).
     * @return The index of the new partition.
     */
    public abstract int create(long size, WellKnownPartitionType type, boolean active);

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
    public abstract int createAligned(WellKnownPartitionType type, boolean active, int alignment);

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
    public abstract int createAligned(long size, WellKnownPartitionType type, boolean active, int alignment);

    /**
     * Deletes a partition at a given index.
     * 
     * @param index The index of the partition.
     */
    public abstract void delete(int index);

}
