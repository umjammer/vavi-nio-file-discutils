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

package DiscUtils.SquashFs;

import java.io.Closeable;
import java.io.IOException;

import DiscUtils.Core.Compression.ZlibStream;
import DiscUtils.Streams.Util.EndianUtilities;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.io.compression.CompressionMode;


final class MetablockWriter implements Closeable {
    private MemoryStream _buffer;

    private final byte[] _currentBlock;

    private int _currentBlockNum;

    private int _currentOffset;

    public MetablockWriter() {
        _currentBlock = new byte[8 * 1024];
        _buffer = new MemoryStream();
    }

    public MetadataRef getPosition() {
        return new MetadataRef(_currentBlockNum, _currentOffset);
    }

    public void close() throws IOException {
        if (_buffer != null) {
            _buffer.close();
            _buffer = null;
        }
    }

    public void write(byte[] buffer, int offset, int count) {
        int totalStored = 0;
        while (totalStored < count) {
            int toCopy = Math.min(_currentBlock.length - _currentOffset, count - totalStored);
            System.arraycopy(buffer, offset + totalStored, _currentBlock, _currentOffset, toCopy);
            _currentOffset += toCopy;
            totalStored += toCopy;
            if (_currentOffset == _currentBlock.length) {
                nextBlock();
                _currentOffset = 0;
            }
        }
    }

    void persist(Stream output) {
        if (_currentOffset > 0) {
            nextBlock();
        }

        output.write(_buffer.toArray(), 0, (int) _buffer.getLength());
    }

    long distanceFrom(MetadataRef startPos) {
        return (_currentBlockNum - startPos.getBlock()) * VfsSquashFileSystemReader.MetadataBufferSize +
            (_currentOffset - startPos.getOffset());
    }

    private void nextBlock() {
        MemoryStream compressed = new MemoryStream();
        try (ZlibStream compStream = new ZlibStream(compressed, CompressionMode.Compress, true)) {
            compStream.write(_currentBlock, 0, _currentOffset);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }

        byte[] writeData;
        int writeLen;
        if (compressed.getLength() < _currentOffset) {
            writeData = compressed.toArray();
            writeLen = (int) (compressed.getLength() & 0xffff);
        } else {
            writeData = _currentBlock;
            writeLen = _currentOffset | 0x8000;
        }

        byte[] header = new byte[2];
        EndianUtilities.writeBytesLittleEndian((short) writeLen, header, 0);
        _buffer.write(header, 0, 2);
        _buffer.write(writeData, 0, writeLen & 0x7FFF);

        ++_currentBlockNum;
    }
}
