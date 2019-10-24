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

package DiscUtils.Registry;

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Range;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * An internal structure within registry files, bins are the major unit of
 * allocation in a registry hive.
 * Bins are divided into multiple cells, that contain actual registry data.
 */
public final class Bin {
    private final byte[] _buffer;

    private final Stream _fileStream;

    private final List<Range> _freeCells;

    private final BinHeader _header;

    private final RegistryHive _hive;

    private final long _streamPos;

    public Bin(RegistryHive hive, Stream stream) {
        _hive = hive;
        _fileStream = stream;
        _streamPos = stream.getPosition();
        stream.setPosition(_streamPos);
        byte[] buffer = StreamUtilities.readExact(stream, 0x20);
        _header = new BinHeader();
        _header.readFrom(buffer, 0);
        _fileStream.setPosition(_streamPos);
        _buffer = StreamUtilities.readExact(_fileStream, _header.BinSize);
        // Gather list of all free cells.
        _freeCells = new ArrayList<>();
        int pos = 0x20;
        while (pos < _buffer.length) {
            int size = EndianUtilities.toInt32LittleEndian(_buffer, pos);
            if (size > 0) {
                _freeCells.add(new Range(pos, size));
            }

            pos += Math.abs(size);
        }
    }

    public Cell tryGetCell(int index) {
        int size = EndianUtilities.toInt32LittleEndian(_buffer, index - _header.FileOffset);
        if (size >= 0) {
            return null;
        }

        return Cell.parse(_hive, index, _buffer, index + 4 - _header.FileOffset);
    }

    public void freeCell(int index) {
        int freeIndex = index - _header.FileOffset;
        int len = EndianUtilities.toInt32LittleEndian(_buffer, freeIndex);
        if (len >= 0) {
            throw new IllegalArgumentException("Attempt to free non-allocated cell");
        }

        len = Math.abs(len);
        // If there's a free cell before this one, combine
        int i = 0;
        while (i < _freeCells.size() && _freeCells.get(i).getOffset() < freeIndex) {
            if (_freeCells.get(i).getOffset() + _freeCells.get(i).getCount() == freeIndex) {
                freeIndex = (int) _freeCells.get(i).getOffset();
                len += _freeCells.get(i).getCount();
                _freeCells.remove(i);
            } else {
                ++i;
            }
        }
        // If there's a free cell after this one, combine
        if (i < _freeCells.size() && _freeCells.get(i).getOffset() == freeIndex + len) {
            len += _freeCells.get(i).getCount();
            _freeCells.remove(i);
        }

        // Record the new free cell
        _freeCells.add(i, new Range(freeIndex, len));
        // Free cells are indicated by length > 0
        EndianUtilities.writeBytesLittleEndian(len, _buffer, freeIndex);
        _fileStream.setPosition(_streamPos + freeIndex);
        _fileStream.write(_buffer, freeIndex, 4);
    }

    public boolean updateCell(Cell cell) {
        int index = cell.getIndex() - _header.FileOffset;
        int allocSize = Math.abs(EndianUtilities.toInt32LittleEndian(_buffer, index));
        int newSize = cell.sizeOf() + 4;
        if (newSize > allocSize) {
            return false;
        }

        cell.writeTo(_buffer, index + 4);
        _fileStream.setPosition(_streamPos + index);
        _fileStream.write(_buffer, index, newSize);
        return true;
    }

    public byte[] readRawCellData(int cellIndex, int maxBytes) {
        int index = cellIndex - _header.FileOffset;
        int len = Math.abs(EndianUtilities.toInt32LittleEndian(_buffer, index));
        byte[] result = new byte[Math.min(len - 4, maxBytes)];
        System.arraycopy(_buffer, index + 4, result, 0, result.length);
        return result;
    }

    public boolean writeRawCellData(int cellIndex, byte[] data, int offset, int count) {
        int index = cellIndex - _header.FileOffset;
        int allocSize = Math.abs(EndianUtilities.toInt32LittleEndian(_buffer, index));
        int newSize = count + 4;
        if (newSize > allocSize) {
            return false;
        }

        System.arraycopy(data, offset, _buffer, index + 4, count);
        _fileStream.setPosition(_streamPos + index);
        _fileStream.write(_buffer, index, newSize);
        return true;
    }

    public int allocateCell(int size) {
        if (size < 8 || size % 8 != 0) {
            throw new IllegalArgumentException("Invalid cell size");
        }

        for (int i = 0; i < _freeCells.size(); ++i) {
            // Very inefficient algorithm - will lead to fragmentation
            int result = (int) (_freeCells.get(i).getOffset() + _header.FileOffset);
            if (_freeCells.get(i).getCount() > size) {
                // Record the newly allocated cell
                EndianUtilities.writeBytesLittleEndian(-size, _buffer, (int) _freeCells.get(i).getOffset());
                _fileStream.setPosition(_streamPos + _freeCells.get(i).getOffset());
                _fileStream.write(_buffer, (int) _freeCells.get(i).getOffset(), 4);
                // Keep the remainder of the free buffer as unallocated
                _freeCells.set(i, new Range(_freeCells.get(i).getOffset() + size, _freeCells.get(i).getCount() - size));
                EndianUtilities
                        .writeBytesLittleEndian(_freeCells.get(i).getCount(), _buffer, (int) _freeCells.get(i).getOffset());
                _fileStream.setPosition(_streamPos + _freeCells.get(i).getOffset());
                _fileStream.write(_buffer, (int) _freeCells.get(i).getOffset(), 4);
                return result;
            }

            if (_freeCells.get(i).getCount() == size) {
                // Record the whole of the free buffer as a newly allocated cell
                EndianUtilities.writeBytesLittleEndian(-size, _buffer, (int) _freeCells.get(i).getOffset());
                _fileStream.setPosition(_streamPos + _freeCells.get(i).getOffset());
                _fileStream.write(_buffer, (int) _freeCells.get(i).getOffset(), 4);
                _freeCells.remove(i);
                return result;
            }
        }
        return -1;
    }
}
