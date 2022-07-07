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
import java.util.List;

import discUtils.streams.StreamExtent;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


public class BiosExtendedPartitionTable {
    private final Stream _disk;

    private final int _firstSector;

    public BiosExtendedPartitionTable(Stream disk, int firstSector) {
        _disk = disk;
        _firstSector = firstSector;
    }

    public List<BiosPartitionRecord> getPartitions() {
        List<BiosPartitionRecord> result = new ArrayList<>();
        int partPos = _firstSector;
        while (partPos != 0) {
            _disk.setPosition((long) partPos * Sizes.Sector);
            byte[] sector = StreamUtilities.readExact(_disk, Sizes.Sector);
            if ((sector[510] & 0xff) != 0x55 || (sector[511] & 0xff) != 0xAA) {
                throw new dotnet4j.io.IOException("Invalid extended partition sector");
            }

            int nextPartPos = 0;
            for (int offset = 0x1BE; offset <= 0x1EE; offset += 0x10) {
                BiosPartitionRecord thisPart = new BiosPartitionRecord(sector, offset, partPos, -1);
                if (thisPart.getStartCylinder() != 0 || thisPart.getStartHead() != 0 || thisPart.getStartSector() != 0
                        || (thisPart.getLBAStart() != 0 && thisPart.getLBALength() != 0)) {
                    if (thisPart.getPartitionType() != 0x05 && thisPart.getPartitionType() != 0x0F) {
                        result.add(thisPart);
                    } else {
                        nextPartPos = _firstSector + (int) thisPart.getLBAStart();
                    }
                }
            }
            partPos = nextPartPos;
        }
        return result;
    }

    /**
     * Gets all of the disk ranges containing partition table data.
     *
     * @return Set of stream extents, indicated as byte offset from the start of the
     *         disk.
     */
    public List<StreamExtent> getMetadataDiskExtents() {
        List<StreamExtent> extents = new ArrayList<>();
        int partPos = _firstSector;
        while (partPos != 0) {
            extents.add(new StreamExtent((long) partPos * Sizes.Sector, Sizes.Sector));
            _disk.setPosition((long) partPos * Sizes.Sector);
            byte[] sector = StreamUtilities.readExact(_disk, Sizes.Sector);
            if ((sector[510] & 0xff) != 0x55 || (sector[511] & 0xff) != 0xAA) {
                throw new dotnet4j.io.IOException("Invalid extended partition sector");
            }

            int nextPartPos = 0;
            for (int offset = 0x1BE; offset <= 0x1EE; offset += 0x10) {
                BiosPartitionRecord thisPart = new BiosPartitionRecord(sector, offset, partPos, -1);
                if (thisPart.getStartCylinder() != 0 || thisPart.getStartHead() != 0 || thisPart.getStartSector() != 0) {
                    if (thisPart.getPartitionType() == 0x05 || thisPart.getPartitionType() == 0x0F) {
                        nextPartPos = _firstSector + (int) thisPart.getLBAStart();
                        break;
                    }
                }
            }
            partPos = nextPartPos;
        }
        return extents;
    }
}
