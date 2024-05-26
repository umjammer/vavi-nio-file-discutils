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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discUtils.core.Geometry;
import discUtils.core.VirtualDisk;
import discUtils.streams.SparseMemoryStream;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.builder.BuilderBufferExtent;
import discUtils.streams.builder.BuilderExtent;
import discUtils.streams.builder.BuilderSparseStreamExtent;
import discUtils.streams.builder.StreamBuilder;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;


/**
 * Builds a stream with the contents of a BIOS partitioned disk.
 *
 * This class assembles a disk image dynamically in memory. The constructed
 * stream will read data from the partition content streams only when a client
 * of this class tries to read from that partition.
 */
public class BiosPartitionedDiskBuilder extends StreamBuilder {

    private Geometry biosGeometry;

    private final SparseMemoryStream bootSectors;

    private final long capacity;

    private final Map<Integer, BuilderExtent> partitionContents;

    /**
     * Initializes a new instance of the BiosPartitionedDiskBuilder class.
     *
     * @param capacity The capacity of the disk (in bytes).
     * @param biosGeometry The BIOS geometry of the disk.
     */
    public BiosPartitionedDiskBuilder(long capacity, Geometry biosGeometry) {
        this.capacity = capacity;
        this.biosGeometry = biosGeometry;
        bootSectors = new SparseMemoryStream();
        bootSectors.setLength(capacity);
        partitionTable = BiosPartitionTable.initialize(bootSectors, this.biosGeometry);
        partitionContents = new HashMap<>();
    }

    /**
     * Initializes a new instance of the BiosPartitionedDiskBuilder class.
     *
     * @param capacity The capacity of the disk (in bytes).
     * @param bootSectors The boot sector(s) of the disk.
     * @param biosGeometry The BIOS geometry of the disk.
     */
    public BiosPartitionedDiskBuilder(long capacity, byte[] bootSectors, Geometry biosGeometry) {
        if (bootSectors == null) {
            throw new IllegalArgumentException("bootSectors");
        }

        this.capacity = capacity;
        this.biosGeometry = biosGeometry;
        this.bootSectors = new SparseMemoryStream();
        this.bootSectors.setLength(capacity);
        this.bootSectors.write(bootSectors, 0, bootSectors.length);
        partitionTable = new BiosPartitionTable(this.bootSectors, biosGeometry);
        partitionContents = new HashMap<>();
    }

    /**
     * Initializes a new instance of the BiosPartitionedDiskBuilder class by
     * cloning the partition structure of a source disk.
     *
     * @param sourceDisk The disk to clone.
     */
    public BiosPartitionedDiskBuilder(VirtualDisk sourceDisk) {
        if (sourceDisk == null) {
            throw new IllegalArgumentException("sourceDisk");
        }

        capacity = sourceDisk.getCapacity();
        biosGeometry = sourceDisk.getBiosGeometry();
        bootSectors = new SparseMemoryStream();
        bootSectors.setLength(capacity);
        for (StreamExtent extent : (new BiosPartitionTable(sourceDisk)).getMetadataDiskExtents()) {
            sourceDisk.getContent().position(extent.getStart());
            byte[] buffer = StreamUtilities.readExact(sourceDisk.getContent(), (int) extent.getLength());
            bootSectors.position(extent.getStart());
            bootSectors.write(buffer, 0, buffer.length);
        }
        partitionTable = new BiosPartitionTable(bootSectors, biosGeometry);
        partitionContents = new HashMap<>();
    }

    /**
     * Gets the partition table in the disk.
     */
    private final BiosPartitionTable partitionTable;

    public BiosPartitionTable getPartitionTable() {
        return partitionTable;
    }

    /**
     * Sets a stream representing the content of a partition in the partition
     * table.
     *
     * @param index The index of the partition.
     * @param stream The stream with the contents of the partition.
     */
    public void setPartitionContent(int index, SparseStream stream) {
        partitionContents.put(index,
                               new BuilderSparseStreamExtent(getPartitionTable().get(index).getFirstSector() *
                                                             Sizes.Sector,
                                                             stream));
    }

    /**
     * Updates the CHS fields in partition records to reflect a new BIOS
     * geometry.
     *
     * The partitions are not relocated to a cylinder boundary, just the CHS
     * fields are updated on the assumption the LBA fields are definitive.
     *
     * @param geometry The disk's new BIOS geometry.
     */
    public void updateBiosGeometry(Geometry geometry) {
        getPartitionTable().updateBiosGeometry(geometry);
        biosGeometry = geometry;
    }

    /**
     * @param totalLength {@cs out}
     */
    @Override protected List<BuilderExtent> fixExtents(long[] totalLength) {
        totalLength[0] = capacity;
        List<BuilderExtent> extents = new ArrayList<>();
        for (StreamExtent extent : getPartitionTable().getMetadataDiskExtents()) {
            bootSectors.position(extent.getStart());
            byte[] buffer = StreamUtilities.readExact(bootSectors, (int) extent.getLength());
            extents.add(new BuilderBufferExtent(extent.getStart(), buffer));
        }
        extents.addAll(partitionContents.values());
        return extents;
    }
}
