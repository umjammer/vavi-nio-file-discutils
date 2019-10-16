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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Core.Partitions.GuidPartitionTable;
import DiscUtils.Core.Partitions.GuidPartitionTypes;
import DiscUtils.Core.Partitions.WellKnownPartitionType;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Vhdx.Disk;
import moe.yo3explorer.dotnetio4j.MemoryStream;


public class GuidPartitionTableTest {
    @Test
    public void initialize() throws Exception {
        MemoryStream ms = new MemoryStream();
        try (Disk disk = Disk.initializeDynamic(ms, Ownership.Dispose, 3 * 1024 * 1024)) {
            GuidPartitionTable table = GuidPartitionTable.initialize(disk);
            assertEquals(0, table.getCount());
        }
    }

    @Test
    public void createSmallWholeDisk() throws Exception {
        MemoryStream ms = new MemoryStream();
        try (Disk disk = Disk.initializeDynamic(ms, Ownership.Dispose, 3 * 1024 * 1024)) {
            GuidPartitionTable table = GuidPartitionTable.initialize(disk);
            int idx = table.create(WellKnownPartitionType.WindowsFat, true);
            // Make sure the partition fills from first to last usable.
            assertEquals(table.getFirstUsableSector(), table.get___idx(idx).getFirstSector());
            assertEquals(table.getLastUsableSector(), table.get___idx(idx).getLastSector());
        }
    }

    @Test
    public void createMediumWholeDisk() throws Exception {
        MemoryStream ms = new MemoryStream();
        try (Disk disk = Disk.initializeDynamic(ms, Ownership.Dispose, 2 * 1024L * 1024 * 1024)) {
            GuidPartitionTable table = GuidPartitionTable.initialize(disk);
            int idx = table.create(WellKnownPartitionType.WindowsFat, true);
            assertEquals(2, table.getPartitions().size());
            assertEquals(GuidPartitionTypes.MicrosoftReserved, table.get___idx(0).getGuidType());
            assertEquals(32 * 1024 * 1024, table.get___idx(0).getSectorCount() * 512);
            // Make sure the partition fills from first to last usable, allowing for
            // MicrosoftReserved sector.
            assertEquals(table.get___idx(0).getLastSector() + 1, table.get___idx(idx).getFirstSector());
            assertEquals(table.getLastUsableSector(), table.get___idx(idx).getLastSector());
        }
    }

    @Test
    public void createLargeWholeDisk() throws Exception {
        MemoryStream ms = new MemoryStream();
        try (Disk disk = Disk.initializeDynamic(ms, Ownership.Dispose, 200 * 1024L * 1024 * 1024)) {
            GuidPartitionTable table = GuidPartitionTable.initialize(disk);
            int idx = table.create(WellKnownPartitionType.WindowsFat, true);
            assertEquals(2, table.getPartitions().size());
            assertEquals(GuidPartitionTypes.MicrosoftReserved, table.get___idx(0).getGuidType());
            assertEquals(128 * 1024 * 1024, table.get___idx(0).getSectorCount() * 512);
            // Make sure the partition fills from first to last usable, allowing for
            // MicrosoftReserved sector.
            assertEquals(table.get___idx(0).getLastSector() + 1, table.get___idx(idx).getFirstSector());
            assertEquals(table.getLastUsableSector(), table.get___idx(idx).getLastSector());
        }
    }

    @Test
    public void createAlignedWholeDisk() throws Exception {
        MemoryStream ms = new MemoryStream();
        try (Disk disk = Disk.initializeDynamic(ms, Ownership.Dispose, 200 * 1024L * 1024 * 1024)) {
            GuidPartitionTable table = GuidPartitionTable.initialize(disk);
            int idx = table.createAligned(WellKnownPartitionType.WindowsFat, true, 1024 * 1024);
            assertEquals(2, table.getPartitions().size());
            assertEquals(GuidPartitionTypes.MicrosoftReserved, table.get___idx(0).getGuidType());
            assertEquals(128 * 1024 * 1024, table.get___idx(0).getSectorCount() * 512);
            // Make sure the partition is aligned
            assertEquals(0, table.get___idx(idx).getFirstSector() % 2048);
            assertEquals(0, (table.get___idx(idx).getLastSector() + 1) % 2048);
            // Ensure partition fills most of the disk
            assertTrue((table.get___idx(idx).getSectorCount() * 512) > disk.getCapacity() * 0.9);
        }
    }

    @Test
    public void createBySize() throws Exception {
        MemoryStream ms = new MemoryStream();
        try (Disk disk = Disk.initializeDynamic(ms, Ownership.Dispose, 3 * 1024 * 1024)) {
            GuidPartitionTable table = GuidPartitionTable.initialize(disk);
            int idx = table.create(2 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false);
            // Make sure the partition is within 10% of the size requested.
            assertTrue((2 * 1024 * 2) * 0.9 < table.get___idx(idx).getSectorCount());
            assertEquals(table.getFirstUsableSector(), table.get___idx(idx).getFirstSector());
        }
    }

    @Test
    public void createBySizeInGap() throws Exception {
        MemoryStream ms = new MemoryStream();
        try (Disk disk = Disk.initializeDynamic(ms, Ownership.Dispose, 300 * 1024 * 1024)) {
            GuidPartitionTable table = GuidPartitionTable.initialize(disk);
            table.create(10 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false);
            table.create((20 * 1024 * 1024)
                    / 512, ((30 * 1024 * 1024) / 512) - 1, GuidPartitionTypes.WindowsBasicData, 0, "Data Partition");
            table.create((60 * 1024 * 1024)
                    / 512, ((70 * 1024 * 1024) / 512) - 1, GuidPartitionTypes.WindowsBasicData, 0, "Data Partition");
            int idx = table.create(20 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false);
            assertEquals(((30 * 1024 * 1024) / 512), table.get___idx(idx).getFirstSector());
            assertEquals(((50 * 1024 * 1024) / 512) - 1, table.get___idx(idx).getLastSector());
        }
    }

    @Test
    public void createBySizeInGapAligned() throws Exception {
        MemoryStream ms = new MemoryStream();
        try (Disk disk = Disk.initializeDynamic(ms, Ownership.Dispose, 300 * 1024 * 1024)) {
            GuidPartitionTable table = GuidPartitionTable.initialize(disk);
            table.create(10 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false);
            // Note: end is unaligned
            table.create((20 * 1024 * 1024)
                    / 512, ((30 * 1024 * 1024) / 512) - 5, GuidPartitionTypes.WindowsBasicData, 0, "Data Partition");
            table.create((60 * 1024 * 1024)
                    / 512, ((70 * 1024 * 1024) / 512) - 1, GuidPartitionTypes.WindowsBasicData, 0, "Data Partition");
            int idx = table.createAligned(20 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false, 64 * 1024);
            assertEquals(((30 * 1024 * 1024) / 512), table.get___idx(idx).getFirstSector());
            assertEquals(((50 * 1024 * 1024) / 512) - 1, table.get___idx(idx).getLastSector());
        }
    }

    @Test
    public void delete() throws Exception {
        MemoryStream ms = new MemoryStream();
        try (Disk disk = Disk.initializeDynamic(ms, Ownership.Dispose, 10 * 1024 * 1024)) {
            GuidPartitionTable table = GuidPartitionTable.initialize(disk);
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
    }
}
