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

package discUtils.registry;

import java.util.ArrayList;
import java.util.List;

import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Range;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * An internal structure within registry files, bins are the major unit of
 * allocation in a registry hive.
 * Bins are divided into multiple cells, that contain actual registry data.
 */
public final class Bin {

    private final byte[] buffer;

    private final Stream fileStream;

    private final List<Range> freeCells;

    private final BinHeader header;

    private final RegistryHive hive;

    private final long streamPos;

    public Bin(RegistryHive hive, Stream stream) {
        this.hive = hive;
        fileStream = stream;
        streamPos = stream.getPosition();
        stream.setPosition(streamPos);
        byte[] buffer = StreamUtilities.readExact(stream, 0x20);
        header = new BinHeader();
        header.readFrom(buffer, 0);
        fileStream.setPosition(streamPos);
        this.buffer = StreamUtilities.readExact(fileStream, header.binSize);
        // Gather list of all free cells.
        freeCells = new ArrayList<>();
        int pos = 0x20;
        while (pos < this.buffer.length) {
            int size = EndianUtilities.toInt32LittleEndian(this.buffer, pos);
            if (size > 0) {
                freeCells.add(new Range(pos, size));
            }

            pos += Math.abs(size);
        }
    }

    public Cell tryGetCell(int index) {
        int size = EndianUtilities.toInt32LittleEndian(buffer, index - header.fileOffset);
        if (size >= 0) {
            return null;
        }

        return Cell.parse(hive, index, buffer, index + 4 - header.fileOffset);
    }

    public void freeCell(int index) {
        int freeIndex = index - header.fileOffset;
        int len = EndianUtilities.toInt32LittleEndian(buffer, freeIndex);
        if (len >= 0) {
            throw new IllegalArgumentException("Attempt to free non-allocated cell");
        }

        len = Math.abs(len);
        // If there's a free cell before this one, combine
        int i = 0;
        while (i < freeCells.size() && freeCells.get(i).getOffset() < freeIndex) {
            if (freeCells.get(i).getOffset() + freeCells.get(i).getCount() == freeIndex) {
                freeIndex = (int) freeCells.get(i).getOffset();
                len += freeCells.get(i).getCount();
                freeCells.remove(i);
            } else {
                ++i;
            }
        }
        // If there's a free cell after this one, combine
        if (i < freeCells.size() && freeCells.get(i).getOffset() == freeIndex + len) {
            len += freeCells.get(i).getCount();
            freeCells.remove(i);
        }

        // Record the new free cell
        freeCells.add(i, new Range(freeIndex, len));
        // Free cells are indicated by length > 0
        EndianUtilities.writeBytesLittleEndian(len, buffer, freeIndex);
        fileStream.setPosition(streamPos + freeIndex);
        fileStream.write(buffer, freeIndex, 4);
    }

    public boolean updateCell(Cell cell) {
        int index = cell.getIndex() - header.fileOffset;
        int allocSize = Math.abs(EndianUtilities.toInt32LittleEndian(buffer, index));
        int newSize = cell.size() + 4;
        if (newSize > allocSize) {
            return false;
        }

        cell.writeTo(buffer, index + 4);
        fileStream.setPosition(streamPos + index);
        fileStream.write(buffer, index, newSize);
        return true;
    }

    public byte[] readRawCellData(int cellIndex, int maxBytes) {
        int index = cellIndex - header.fileOffset;
        int len = Math.abs(EndianUtilities.toInt32LittleEndian(buffer, index));
        byte[] result = new byte[Math.min(len - 4, maxBytes)];
        System.arraycopy(buffer, index + 4, result, 0, result.length);
        return result;
    }

    public boolean writeRawCellData(int cellIndex, byte[] data, int offset, int count) {
        int index = cellIndex - header.fileOffset;
        int allocSize = Math.abs(EndianUtilities.toInt32LittleEndian(buffer, index));
        int newSize = count + 4;
        if (newSize > allocSize) {
            return false;
        }

        System.arraycopy(data, offset, buffer, index + 4, count);
        fileStream.setPosition(streamPos + index);
        fileStream.write(buffer, index, newSize);
        return true;
    }

    public int allocateCell(int size) {
        if (size < 8 || size % 8 != 0) {
            throw new IllegalArgumentException("Invalid cell size");
        }

        for (int i = 0; i < freeCells.size(); ++i) {
            // Very inefficient algorithm - will lead to fragmentation
            int result = (int) (freeCells.get(i).getOffset() + header.fileOffset);
            if (freeCells.get(i).getCount() > size) {
                // Record the newly allocated cell
                EndianUtilities.writeBytesLittleEndian(-size, buffer, (int) freeCells.get(i).getOffset());
                fileStream.setPosition(streamPos + freeCells.get(i).getOffset());
                fileStream.write(buffer, (int) freeCells.get(i).getOffset(), 4);
                // Keep the remainder of the free buffer as unallocated
                freeCells.set(i, new Range(freeCells.get(i).getOffset() + size, freeCells.get(i).getCount() - size));
                EndianUtilities
                        .writeBytesLittleEndian(freeCells.get(i).getCount(), buffer, (int) freeCells.get(i).getOffset());
                fileStream.setPosition(streamPos + freeCells.get(i).getOffset());
                fileStream.write(buffer, (int) freeCells.get(i).getOffset(), 4);
                return result;
            }

            if (freeCells.get(i).getCount() == size) {
                // Record the whole of the free buffer as a newly allocated cell
                EndianUtilities.writeBytesLittleEndian(-size, buffer, (int) freeCells.get(i).getOffset());
                fileStream.setPosition(streamPos + freeCells.get(i).getOffset());
                fileStream.write(buffer, (int) freeCells.get(i).getOffset(), 4);
                freeCells.remove(i);
                return result;
            }
        }
        return -1;
    }
}
