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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import discUtils.core.Geometry;
import discUtils.core.partitions.BiosPartitionTable;
import discUtils.core.partitions.BiosPartitionedDiskBuilder;
import discUtils.core.partitions.WellKnownPartitionType;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;


public class BiosPartitionedDiskBuilderTest {
    @Test
    public void basic() throws Exception {
        long capacity = 10 * 1024 * 1024;
        Geometry geometry = Geometry.fromCapacity(capacity);
        BiosPartitionedDiskBuilder builder = new BiosPartitionedDiskBuilder(capacity, geometry);
        builder.getPartitionTable().create(WellKnownPartitionType.WindowsNtfs, true);
        SparseStream partitionContent = SparseStream
                .fromStream(new MemoryStream((int) (builder.getPartitionTable().get(0).getSectorCount() * 512)),
                            Ownership.Dispose);
        partitionContent.position(4053);
        partitionContent.writeByte((byte) 0xAf);
        builder.setPartitionContent(0, partitionContent);
        SparseStream constructedStream = builder.build();
        BiosPartitionTable bpt = new BiosPartitionTable(constructedStream, geometry);
        assertEquals(1, bpt.getCount());

        try (Stream builtPartitionStream = bpt.getPartitions().get(0).open()) {
            builtPartitionStream.position(4053);
            assertEquals(0xAf, builtPartitionStream.readByte() & 0xff);
        }
    }
}
