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

package DiscUtils.Vdi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.SeekOrigin;
import moe.yo3explorer.dotnetio4j.Stream;


public class DiskStream extends SparseStream {
    private static final int BlockFree = 0;

    private static final int BlockZero = 1;

    private boolean _atEof;

    private int[] _blockTable;

    private final HeaderRecord _fileHeader;

    private Stream _fileStream;

    private boolean _isDisposed;

    private final Ownership _ownsStream;

    private long _position;

    private boolean _writeNotified;

    public DiskStream(Stream fileStream, Ownership ownsStream, HeaderRecord fileHeader) {
        _fileStream = fileStream;
        _fileHeader = fileHeader;
        _ownsStream = ownsStream;
        readBlockTable();
    }

    public boolean canRead() {
        checkDisposed();
        return true;
    }

    public boolean canSeek() {
        checkDisposed();
        return true;
    }

    public boolean canWrite() {
        checkDisposed();
        return _fileStream.canWrite();
    }

    public List<StreamExtent> getExtents() {
        List<StreamExtent> extents = new ArrayList<>();
        long blockSize = _fileHeader.blockSize;
        int i = 0;
        while (i < _blockTable.length) {
            while (i < _blockTable.length && (_blockTable[i] == BlockZero || _blockTable[i] == BlockFree)) {
                // Find next stored block
                ++i;
            }
            int start = i;
            while (i < _blockTable.length && _blockTable[i] != BlockZero && _blockTable[i] != BlockFree) {
                // Find next absent block
                ++i;
            }
            if (start != i) {
                extents.add(new StreamExtent(start * blockSize, (i - start) * blockSize));
            }

        }
        return extents;
    }

    public long getLength() {
        checkDisposed();
        return _fileHeader.diskSize;
    }

    public long getPosition() {
        checkDisposed();
        return _position;
    }

    public void setPosition(long value) {
        checkDisposed();
        _position = value;
        _atEof = false;
    }

    public BiConsumer<Object, Object[]> WriteOccurred;

    public void flush() {
        checkDisposed();
    }

    public int read(byte[] buffer, int offset, int count) {
        checkDisposed();
//Debug.println(_position + ", " + _fileHeader.diskSize);
        if (_atEof || _position > _fileHeader.diskSize) {
            _atEof = true;
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to read beyond end of file");
        }

        if (_position == _fileHeader.diskSize) {
            _atEof = true;
            return 0;
        }

        int maxToRead = (int) Math.min(count, _fileHeader.diskSize - _position);
        int numRead = 0;
        while (numRead < maxToRead) {
            int block = (int) (_position / _fileHeader.blockSize);
            int offsetInBlock = (int) (_position % _fileHeader.blockSize);
            int toRead = Math.min(maxToRead - numRead, _fileHeader.blockSize - offsetInBlock);
            if (_blockTable[block] == BlockFree) {
                // TODO: Use parent
                Arrays.fill(buffer, offset + numRead, offset + numRead + toRead, (byte) 0);
            } else if (_blockTable[block] == BlockZero) {
                Arrays.fill(buffer, offset + numRead, offset + numRead + toRead, (byte) 0);
            } else {
                // TODO _blockTable[block] got negative
                long blockOffset = _blockTable[block] * (_fileHeader.blockSize + _fileHeader.blockExtraSize);
                long filePos = _fileHeader.dataOffset + _fileHeader.blockExtraSize + blockOffset + offsetInBlock;
                _fileStream.setPosition(filePos);
                StreamUtilities.readExact(_fileStream, buffer, offset + numRead, toRead);
            }
            _position += toRead;
            numRead += toRead;
        }
        return numRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        checkDisposed();
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += _position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += _fileHeader.diskSize;
        }

        _atEof = false;
        if (effectiveOffset < 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to move before beginning of disk");
        }

        _position = effectiveOffset;
        return _position;
    }

    public void setLength(long value) {
        checkDisposed();
        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        checkDisposed();
        if (!canWrite()) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to write to read-only stream");
        }

        if (count < 0) {
            throw new IndexOutOfBoundsException("Attempt to write negative number of bytes (count)");
        }

        if (_atEof || _position + count > _fileHeader.diskSize) {
            _atEof = true;
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to write beyond end of file");
        }

        // On first write, notify event listeners - they just get to find out that some
        // write occurred, not about each write.
        if (!_writeNotified) {
            onWriteOccurred();
            _writeNotified = true;
        }

        int numWritten = 0;
        while (numWritten < count) {
            int block = (int) (_position / _fileHeader.blockSize);
            int offsetInBlock = (int) (_position % _fileHeader.blockSize);
            int toWrite = Math.min(count - numWritten, _fileHeader.blockSize - offsetInBlock);
            // Optimize away zero-writes
            if (_blockTable[block] == BlockZero || (_blockTable[block] == BlockFree && toWrite == _fileHeader.blockSize)) {
                if (Utilities.isAllZeros(buffer, offset + numWritten, toWrite)) {
                    numWritten += toWrite;
                    _position += toWrite;
                    continue;
                }
            }

            if (_blockTable[block] == BlockFree || _blockTable[block] == BlockZero) {
                byte[] writeBuffer = buffer;
                int writeBufferOffset = offset + numWritten;
                if (toWrite != _fileHeader.blockSize) {
                    writeBuffer = new byte[_fileHeader.blockSize];
                    if (_blockTable[block] == BlockFree) {
                        // TODO: Use parent stream data...
                    }

                    // Copy actual data into temporary buffer, then this is a full block write.
                    System.arraycopy(buffer, offset + numWritten, writeBuffer, offsetInBlock, toWrite);
                    writeBufferOffset = 0;
                }

                long blockOffset = (long) _fileHeader.blocksAllocated * (_fileHeader.blockSize + _fileHeader.blockExtraSize);
                long filePos = _fileHeader.dataOffset + _fileHeader.blockExtraSize + blockOffset;
                _fileStream.setPosition(filePos);
                _fileStream.write(writeBuffer, writeBufferOffset, _fileHeader.blockSize);
                _blockTable[block] = _fileHeader.blocksAllocated;
                // Update the file header on disk, to indicate where the next free block is
                _fileHeader.blocksAllocated++;
                _fileStream.setPosition(PreHeaderRecord.Size);
                _fileHeader.write(_fileStream);
                // Update the block table on disk, to indicate where this block is
                writeBlockTableEntry(block);
            } else {
                // Existing block, simply overwrite the existing data
                long blockOffset = _blockTable[block] * (_fileHeader.blockSize + _fileHeader.blockExtraSize);
                long filePos = _fileHeader.dataOffset + _fileHeader.blockExtraSize + blockOffset + offsetInBlock;
                _fileStream.setPosition(filePos);
                _fileStream.write(buffer, offset + numWritten, toWrite);
            }
            numWritten += toWrite;
            _position += toWrite;
        }
    }

    public void close() throws IOException {
        _isDisposed = true;
        if (_ownsStream == Ownership.Dispose && _fileStream != null) {
            _fileStream.close();
            _fileStream = null;
        }
    }

    protected void onWriteOccurred() {
        if (WriteOccurred != null) {
            WriteOccurred.accept(this, null);
        }

    }

    private void readBlockTable() {
        _fileStream.setPosition(_fileHeader.blocksOffset);
        byte[] buffer = StreamUtilities.readExact(_fileStream, _fileHeader.blockCount * 4);
        _blockTable = new int[_fileHeader.blockCount];
        for (int i = 0; i < _fileHeader.blockCount; ++i) {
            _blockTable[i] = EndianUtilities.toUInt32LittleEndian(buffer, i * 4); // TODO negative
System.err.println(_blockTable[i]);
        }
    }

    private void writeBlockTableEntry(int block) {
        byte[] buffer = new byte[4];
        EndianUtilities.writeBytesLittleEndian(_blockTable[block], buffer, 0);
        _fileStream.setPosition(_fileHeader.blocksOffset + block * 4);
        _fileStream.write(buffer, 0, 4);
    }

    private void checkDisposed() {
        if (_isDisposed) {
            throw new moe.yo3explorer.dotnetio4j.IOException("DiskStream: Attempt to use disposed stream");
        }
    }
}
