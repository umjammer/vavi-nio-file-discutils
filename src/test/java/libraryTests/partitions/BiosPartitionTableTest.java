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

package libraryTests.partitions;

import discUtils.core.ChsAddress;
import discUtils.core.Geometry;
import discUtils.core.partitions.BiosPartitionInfo;
import discUtils.core.partitions.BiosPartitionTable;
import discUtils.core.partitions.BiosPartitionTypes;
import discUtils.core.partitions.WellKnownPartitionType;
import discUtils.streams.SparseMemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiosPartitionTableTest {

    @Test
    void initialize() throws Exception {
        long capacity = 3 * 1024 * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.fromCapacity(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        assertEquals(0, table.getCount());
    }

    @Test
    void createWholeDisk() throws Exception {
        long capacity = 3 * 1024 * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.fromCapacity(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        int idx = table.create(WellKnownPartitionType.WindowsFat, true);
        // Make sure the partition fills all but the first track on the disk.
        assertEquals(geom.getTotalSectorsLong(), table.get(idx).getSectorCount() + geom.getSectorsPerTrack());
        // Make sure FAT16 was selected for a disk of this size
        assertEquals(BiosPartitionTypes.Fat16, table.get(idx).getBiosType());
        // Make sure partition starts where expected
        assertEquals(new ChsAddress(0, 1, 1), ((BiosPartitionInfo) table.get(idx)).getStart());
        // Make sure partition ends at end of disk
        assertEquals(geom.toLogicalBlockAddress(geom.getLastSector()), table.get(idx).getLastSector());
        assertEquals(geom.getLastSector(), ((BiosPartitionInfo) table.get(idx)).getEnd());
        // Make sure the 'active' flag made it through...
        assertTrue(((BiosPartitionInfo) table.get(idx)).isActive());
        // Make sure the partition index is Zero
        assertEquals(0, ((BiosPartitionInfo) table.get(idx)).getPrimaryIndex());
    }

    @Test
    void createWholeDiskAligned() throws Exception {
        long capacity = 3 * 1024 * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.fromCapacity(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        int idx = table.createAligned(WellKnownPartitionType.WindowsFat, true, 64 * 1024);
        assertEquals(0, table.get(idx).getFirstSector() % 128);
        assertEquals(0, (table.get(idx).getLastSector() + 1) % 128);
        assertTrue(table.get(idx).getSectorCount() * 512 > capacity * 0.9);
        // Make sure the partition index is Zero
        assertEquals(0, ((BiosPartitionInfo) table.get(idx)).getPrimaryIndex());
    }

    @Test
    void createBySize() throws Exception {
        long capacity = 3 * 1024 * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.fromCapacity(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        int idx = table.create(2 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false);
        // Make sure the partition is within 10% of the size requested.
        assertTrue((2 * 1024 * 2) * 0.9 < table.get(idx).getSectorCount());
        assertEquals(geom.toLogicalBlockAddress(new ChsAddress(0, 1, 1)), table.get(idx).getFirstSector());
        assertEquals(geom.getHeadsPerCylinder() - 1, geom.toChsAddress((int) table.get(idx).getLastSector()).getHead());
        assertEquals(geom.getSectorsPerTrack(), geom.toChsAddress((int) table.get(idx).getLastSector()).getSector());
    }

    @Test
    void createBySizeInGap() throws Exception {
        SparseMemoryStream ms = new SparseMemoryStream();
        Geometry geom = new Geometry(15, 30, 63);
        ms.setLength(geom.getCapacity());
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        assertEquals(0, table.createPrimaryByCylinder(0, 4, (byte) 33, false));
        assertEquals(1, table.createPrimaryByCylinder(10, 14, (byte) 33, false));
        table.create(geom.toLogicalBlockAddress(new ChsAddress(4, 0, 1)) * 512, WellKnownPartitionType.WindowsFat, true);
    }

    @Test
    void createBySizeInGapAligned() throws Exception {
        SparseMemoryStream ms = new SparseMemoryStream();
        Geometry geom = new Geometry(15, 30, 63);
        ms.setLength(geom.getCapacity());
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        assertEquals(0, table.createPrimaryByCylinder(0, 4, (byte) 33, false));
        assertEquals(1, table.createPrimaryByCylinder(10, 14, (byte) 33, false));
        int idx = table.createAligned(3 * 1024 * 1024, WellKnownPartitionType.WindowsFat, true, 64 * 1024);
        assertEquals(2, idx);
        assertEquals(0, table.get(idx).getFirstSector() % 128);
        assertEquals(0, (table.get(idx).getLastSector() + 1) % 128);
    }

    @Test
    void createByCylinder() throws Exception {
        SparseMemoryStream ms = new SparseMemoryStream();
        Geometry geom = new Geometry(15, 30, 63);
        ms.setLength(geom.getCapacity());
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        assertEquals(0, table.createPrimaryByCylinder(0, 4, (byte) 33, false));
        assertEquals(1, table.createPrimaryByCylinder(10, 14, (byte) 33, false));
        assertEquals(geom.toLogicalBlockAddress(new ChsAddress(0, 1, 1)), table.get(0).getFirstSector());
        assertEquals(geom.toLogicalBlockAddress(new ChsAddress(5, 0, 1)) - 1, table.get(0).getLastSector());
        assertEquals(geom.toLogicalBlockAddress(new ChsAddress(10, 0, 1)), table.get(1).getFirstSector());
        assertEquals(geom.toLogicalBlockAddress(new ChsAddress(14, 29, 63)), table.get(1).getLastSector());
    }

    @Test
    void delete() throws Exception {
        long capacity = 10 * 1024 * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.fromCapacity(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        assertEquals(0, table.create(1 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false));
        assertEquals(1, table.create(2 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false));
        assertEquals(2, table.create(3 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false));
        long[] sectorCount = new long[] {
            table.get(0).getSectorCount(), table.get(1).getSectorCount(), table.get(2).getSectorCount()
        };
        table.delete(1);
        assertEquals(2, table.getCount());
        assertEquals(sectorCount[2], table.get(1).getSectorCount());
    }

    @Test
    void setActive() throws Exception {
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
        assertFalse(((BiosPartitionInfo) table.getPartitions().get(1)).isActive());
        assertTrue(((BiosPartitionInfo) table.getPartitions().get(2)).isActive());
    }

    @Test
    void largeDisk() throws Exception {
        long capacity = 300 * 1024L * 1024L * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.lbaAssistedBiosGeometry(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        table.create(150 * 1024L * 1024L * 1024, WellKnownPartitionType.WindowsNtfs, false);
        table.create(20 * 1024L * 1024L * 1024, WellKnownPartitionType.WindowsNtfs, false);
        table.create(20 * 1024L * 1024L * 1024, WellKnownPartitionType.WindowsNtfs, false);
        assertEquals(3, table.getPartitions().size());
        assertTrue(table.get(0).getSectorCount() * 512L > 140 * 1024L * 1024L * 1024);
        assertTrue(table.get(1).getSectorCount() * 512L > 19 * 1024L * 1024L * 1024);
        assertTrue(table.get(2).getSectorCount() * 512L > 19 * 1024L * 1024L * 1024);
        assertTrue(table.get(0).getFirstSector() > 0);
        assertTrue(table.get(1).getFirstSector() > table.get(0).getLastSector());
        assertTrue(table.get(2).getFirstSector() > table.get(1).getLastSector());
    }

    @Test
    void veryLargePartition() throws Exception {
        long capacity = 1300 * 1024L * 1024L * 1024;
        SparseMemoryStream ms = new SparseMemoryStream();
        ms.setLength(capacity);
        Geometry geom = Geometry.lbaAssistedBiosGeometry(capacity);
        BiosPartitionTable table = BiosPartitionTable.initialize(ms, geom);
        // exception occurs here
        int i = table.createPrimaryByCylinder(0, 150000, (byte) WellKnownPartitionType.WindowsNtfs.ordinal(), true);
        assertEquals(150000, geom.toChsAddress(table.get(0).getLastSector()).getCylinder());
    }
}
