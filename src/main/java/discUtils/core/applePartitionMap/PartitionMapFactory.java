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

package discUtils.core.applePartitionMap;

import discUtils.core.VirtualDisk;
import discUtils.core.partitions.PartitionTable;
import discUtils.core.partitions.PartitionTableFactory;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


public final class PartitionMapFactory implements PartitionTableFactory {
    public boolean detectIsPartitioned(Stream s) {
        if (s.getLength() < 1024) {
            return false;
        }

        s.setPosition(0);

        byte[] initialBytes = StreamUtilities.readExact(s, 1024);

        BlockZero b0 = new BlockZero();
        b0.readFrom(initialBytes, 0);
        if (b0.signature != 0x4552) {
            return false;
        }

        PartitionMapEntry initialPart = new PartitionMapEntry(s);
        initialPart.readFrom(initialBytes, 512);

        return initialPart.signature == 0x504d;
    }

    public PartitionTable detectPartitionTable(VirtualDisk disk) {
        if (!detectIsPartitioned(disk.getContent())) {
            return null;
        }

        return new PartitionMap(disk.getContent());
    }
}
