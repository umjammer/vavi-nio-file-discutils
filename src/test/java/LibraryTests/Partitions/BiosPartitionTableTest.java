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

package LibraryTests.Partitions;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Core.ChsAddress;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.Partitions.BiosPartitionInfo;
import DiscUtils.Core.Partitions.BiosPartitionTable;
import DiscUtils.Core.Partitions.BiosPartitionTypes;
import DiscUtils.Core.Partitions.WellKnownPartitionType;
import DiscUtils.Streams.SparseMemoryStream;

public class BiosPartitionTableTest {
    @Test
    public void initialize() throws Exception {
        long capacity = 3 * 1024 * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.fromCapacity(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        assertEquals(0, table.getCount());
    }

    @Test
    public void createWholeDisk() throws Exception {
        long capacity = 3 * 1024 * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.fromCapacity(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        int idx = table.create(WellKnownPartitionType.WindowsFat, true);
        // Make sure the partition fills all but the first track on the disk.
        assertEquals(geom.getTotalSectorsLong(), table.get___idx(idx).getSectorCount() + geom.getSectorsPerTrack());
        // Make sure FAT16 was selected for a disk of this size
        assertEquals(BiosPartitionTypes.Fat16, table.get___idx(idx).getBiosType());
        // Make sure partition starts where expected
        assertEquals(new ChsAddress(0, 1, 1), ((BiosPartitionInfo) table.get___idx(idx)).getStart());
        // Make sure partition ends at end of disk
        assertEquals(geom.toLogicalBlockAddress(geom.getLastSector()), table.get___idx(idx).getLastSector());
        assertEquals(geom.getLastSector(), ((BiosPartitionInfo) table.get___idx(idx)).getEnd());
        // Make sure the 'active' flag made it through...
        assertTrue(((BiosPartitionInfo) table.get___idx(idx)).getIsActive());
        // Make sure the partition index is Zero
        assertEquals(0, ((BiosPartitionInfo) table.get___idx(idx)).getPrimaryIndex());
    }

    @Test
    public void createWholeDiskAligned() throws Exception {
        long capacity = 3 * 1024 * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.fromCapacity(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        int idx = table.createAligned(WellKnownPartitionType.WindowsFat, true, 64 * 1024);
        assertEquals(0, table.get___idx(idx).getFirstSector() % 128);
        assertEquals(0, (table.get___idx(idx).getLastSector() + 1) % 128);
        assertTrue(table.get___idx(idx).getSectorCount() * 512 > capacity * 0.9);
        // Make sure the partition index is Zero
        assertEquals(0, ((BiosPartitionInfo) table.get___idx(idx)).getPrimaryIndex());
    }

    @Test
    public void createBySize() throws Exception {
        long capacity = 3 * 1024 * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.fromCapacity(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        int idx = table.create(2 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false);
        // Make sure the partition is within 10% of the size requested.
        assertTrue((2 * 1024 * 2) * 0.9 < table.get___idx(idx).getSectorCount());
        assertEquals(geom.toLogicalBlockAddress(new ChsAddress(0, 1, 1)), table.get___idx(idx).getFirstSector());
        assertEquals(geom.getHeadsPerCylinder() - 1, geom.toChsAddress((int) table.get___idx(idx).getLastSector()).getHead());
        assertEquals(geom.getSectorsPerTrack(), geom.toChsAddress((int) table.get___idx(idx).getLastSector()).getSector());
    }

    @Test
    public void createBySizeInGap() throws Exception {
        SparseMemoryStream ms = new SparseMemoryStream();
        Geometry geom = new Geometry(15, 30, 63);
        ms.setLength(geom.getCapacity());
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        assertEquals(0, table.createPrimaryByCylinder(0, 4, (byte) 33, false));
        assertEquals(1, table.createPrimaryByCylinder(10, 14, (byte) 33, false));
        table.create(geom.toLogicalBlockAddress(new ChsAddress(4, 0, 1)) * 512, WellKnownPartitionType.WindowsFat, true);
    }

    @Test
    public void createBySizeInGapAligned() throws Exception {
        SparseMemoryStream ms = new SparseMemoryStream();
        Geometry geom = new Geometry(15, 30, 63);
        ms.setLength(geom.getCapacity());
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        assertEquals(0, table.createPrimaryByCylinder(0, 4, (byte) 33, false));
        assertEquals(1, table.createPrimaryByCylinder(10, 14, (byte) 33, false));
        int idx = table.createAligned(3 * 1024 * 1024, WellKnownPartitionType.WindowsFat, true, 64 * 1024);
        assertEquals(2, idx);
        assertEquals(0, table.get___idx(idx).getFirstSector() % 128);
        assertEquals(0, (table.get___idx(idx).getLastSector() + 1) % 128);
    }

    @Test
    public void createByCylinder() throws Exception {
        SparseMemoryStream ms = new SparseMemoryStream();
        Geometry geom = new Geometry(15, 30, 63);
        ms.setLength(geom.getCapacity());
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        assertEquals(0, table.createPrimaryByCylinder(0, 4, (byte) 33, false));
        assertEquals(1, table.createPrimaryByCylinder(10, 14, (byte) 33, false));
        assertEquals(geom.toLogicalBlockAddress(new ChsAddress(0, 1, 1)), table.get___idx(0).getFirstSector());
        assertEquals(geom.toLogicalBlockAddress(new ChsAddress(5, 0, 1)) - 1, table.get___idx(0).getLastSector());
        assertEquals(geom.toLogicalBlockAddress(new ChsAddress(10, 0, 1)), table.get___idx(1).getFirstSector());
        assertEquals(geom.toLogicalBlockAddress(new ChsAddress(14, 29, 63)), table.get___idx(1).getLastSector());
    }

    @Test
    public void delete() throws Exception {
        long capacity = 10 * 1024 * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.fromCapacity(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        assertEquals(0, table.create(1 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false));
        assertEquals(1, table.create(2 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false));
        assertEquals(2, table.create(3 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false));
        long[] sectorCount = new long[] {
            table.get___idx(0).getSectorCount(), table.get___idx(1).getSectorCount(), table.get___idx(2).getSectorCount()
        };
        table.delete(1);
        assertEquals(2, table.getCount());
        assertEquals(sectorCount[2], table.get___idx(1).getSectorCount());
    }

    @Test
    public void setActive() throws Exception {
        long capacity = 10 * 1024 * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.fromCapacity(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        table.create(1 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false);
        table.create(2 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false);
        table.create(3 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false);
        table.setActivePartition(1);
        table.setActivePartition(2);
        assertFalse(((BiosPartitionInfo) table.getPartitions().get(1)).getIsActive());
        assertTrue(((BiosPartitionInfo) table.getPartitions().get(2)).getIsActive());
    }

    @Test
    public void largeDisk() throws Exception {
        long capacity = 300 * 1024L * 1024L * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.lbaAssistedBiosGeometry(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        table.create(150 * 1024L * 1024L * 1024, WellKnownPartitionType.WindowsNtfs, false);
        table.create(20 * 1024L * 1024L * 1024, WellKnownPartitionType.WindowsNtfs, false); // TODO
        table.create(20 * 1024L * 1024L * 1024, WellKnownPartitionType.WindowsNtfs, false);
        assertEquals(3, table.getPartitions().size());
        assertTrue(table.get___idx(0).getSectorCount() * 512L > 140 * 1024L * 1024L * 1024);
        assertTrue(table.get___idx(1).getSectorCount() * 512L > 19 * 1024L * 1024L * 1024);
        assertTrue(table.get___idx(2).getSectorCount() * 512L > 19 * 1024L * 1024L * 1024);
        assertTrue(table.get___idx(0).getFirstSector() > 0);
        assertTrue(table.get___idx(1).getFirstSector() > table.get___idx(0).getLastSector());
        assertTrue(table.get___idx(2).getFirstSector() > table.get___idx(1).getLastSector());
    }

    @Test
    public void veryLargePartition() throws Exception {
        long capacity = 1300 * 1024L * 1024L * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.lbaAssistedBiosGeometry(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        // exception occurs here
        int i = table.createPrimaryByCylinder(0, 150000, (byte) WellKnownPartitionType.WindowsNtfs.ordinal(), true);
        assertEquals(150000, geom.toChsAddress(table.get___idx(0).getLastSector()).getCylinder());
    }
}
