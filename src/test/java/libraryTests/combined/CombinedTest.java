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

package libraryTests.combined;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import discUtils.core.VirtualDisk;
import discUtils.core.partitions.BiosPartitionTable;
import discUtils.core.partitions.PartitionTable;
import discUtils.core.partitions.WellKnownPartitionType;
import discUtils.fat.FatFileSystem;
import discUtils.streams.util.Ownership;
import discUtils.vhdx.Disk;
import dotnet4j.io.MemoryStream;


public class CombinedTest {
    @Test
    public void simpleVhdFat() throws Exception {
        try (Disk disk = Disk.initializeDynamic(new MemoryStream(), Ownership.Dispose, 16 * 1024 * 1024)) {
            BiosPartitionTable.initialize(disk, WellKnownPartitionType.WindowsFat);
            try (FatFileSystem fs = FatFileSystem.formatPartition(disk, 0, null)) {
                fs.createDirectory("Foo");
            }
        }
    }

    @Test
    public void formatSecondFatPartition() throws Exception {
        MemoryStream ms = new MemoryStream();
        VirtualDisk disk = Disk.initializeDynamic(ms, Ownership.Dispose, 30 * 1024 * 1204);
        PartitionTable pt = BiosPartitionTable.initialize(disk);
        pt.create(15 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false);
        pt.create(5 * 1024 * 1024, WellKnownPartitionType.WindowsFat, false);
        FatFileSystem fileSystem = FatFileSystem.formatPartition(disk, 1, null);
        long fileSystemSize = fileSystem.getTotalSectors() * fileSystem.getBytesPerSector();
        assertTrue(fileSystemSize > (5 * 1024 * 1024) * 0.9);
    }
}
