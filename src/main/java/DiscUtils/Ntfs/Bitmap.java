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

package DiscUtils.Ntfs;

import java.io.Closeable;
import java.io.IOException;

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Block.BlockCacheStream;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.Stream;


final class Bitmap implements Closeable {
    private BlockCacheStream _bitmap;

    private final long _maxIndex;

    private long _nextAvailable;

    private final Stream _stream;

    public Bitmap(Stream stream, long maxIndex) {
        _stream = stream;
        _maxIndex = maxIndex;
        _bitmap = new BlockCacheStream(SparseStream.fromStream(stream, Ownership.None), Ownership.None);
    }

    public void close() throws IOException {
        if (_bitmap != null) {
            _bitmap.close();
            _bitmap = null;
        }
    }

    public boolean isPresent(long index) {
        long byteIdx = index / 8;
        int mask = 1 << (int) (index % 8);
        return (getByte(byteIdx) & mask) != 0;
    }

    public void markPresent(long index) {
        long byteIdx = index / 8;
        byte mask = (byte) (1 << (byte) (index % 8));

        if (byteIdx >= _bitmap.getLength()) {
            _bitmap.setPosition(MathUtilities.roundUp(byteIdx + 1, 8) - 1);
            _bitmap.writeByte((byte) 0);
        }

        setByte(byteIdx, (byte) (getByte(byteIdx) | mask));
    }

    public void markPresentRange(long index, long count) {
        if (count <= 0) {
            return;
        }

        long firstByte = index / 8;
        long lastByte = (index + count - 1) / 8;

        if (lastByte >= _bitmap.getLength()) {
            _bitmap.setPosition(MathUtilities.roundUp(lastByte + 1, 8) - 1);
            _bitmap.writeByte((byte) 0);
        }

        byte[] buffer = new byte[(int) (lastByte - firstByte + 1)];
        buffer[0] = getByte(firstByte);
        if (buffer.length != 1) {
            buffer[buffer.length - 1] = getByte(lastByte);
        }

        for (long i = index; i < index + count; ++i) {
            long byteIdx = i / 8 - firstByte;
            byte mask = (byte) (1 << (byte) (i % 8));
            buffer[(int) byteIdx] |= mask;
        }

        setBytes(firstByte, buffer);
    }

    public void markAbsent(long index) {
        long byteIdx = index / 8;
        byte mask = (byte) (1 << (byte) (index % 8));

        if (byteIdx < _stream.getLength()) {
            setByte(byteIdx, (byte) (getByte(byteIdx) & ~mask));
        }

        if (index < _nextAvailable) {
            _nextAvailable = index;
        }
    }

    void markAbsentRange(long index, long count) {
        if (count <= 0) {
            return;
        }

        long firstByte = index / 8;
        long lastByte = (index + count - 1) / 8;
        if (lastByte >= _bitmap.getLength()) {
            _bitmap.setPosition(MathUtilities.roundUp(lastByte + 1, 8) - 1);
            _bitmap.writeByte((byte) 0);
        }

        byte[] buffer = new byte[(int) (lastByte - firstByte + 1)];
        buffer[0] = getByte(firstByte);
        if (buffer.length != 1) {
            buffer[buffer.length - 1] = getByte(lastByte);
        }

        for (long i = index; i < index + count; ++i) {
            long byteIdx = i / 8 - firstByte;
            byte mask = (byte) (1 << (byte) (i % 8));

            buffer[(int) byteIdx] &= (byte) ~mask;
        }

        setBytes(firstByte, buffer);

        if (index < _nextAvailable) {
            _nextAvailable = index;
        }
    }

    long allocateFirstAvailable(long minValue) {
        long i = Math.max(minValue, _nextAvailable);
        while (isPresent(i) && i < _maxIndex) {
            ++i;
        }

        if (i < _maxIndex) {
            markPresent(i);
            _nextAvailable = i + 1;
            return i;
        }
        return -1;
    }

    long setTotalEntries(long numEntries) {
        long length = MathUtilities.roundUp(MathUtilities.ceil(numEntries, 8), 8);
        _stream.setLength(length);
        return length * 8;
    }

    long getSize() {
        return _bitmap.getLength();
    }

    byte getByte(long index) {
        if (index >= _bitmap.getLength()) {
            return 0;
        }

        byte[] buffer = new byte[1];
        _bitmap.setPosition(index);
        if (_bitmap.read(buffer, 0, 1) != 0) {
            return buffer[0];
        }

        return 0;
    }

    int getBytes(long index, byte[] buffer, int offset, int count) {
        if (index + count >= _bitmap.getLength())
            count = (int) (_bitmap.getLength() - index);
        if (count <= 0)
            return 0;
        _bitmap.setPosition(index);
        return _bitmap.read(buffer, offset, count);
    }

    private void setByte(long index, byte value) {
        byte[] buffer = new byte[] {
            value
        };
        _bitmap.setPosition(index);
        _bitmap.write(buffer, 0, 1);
        _bitmap.flush();
    }

    private void setBytes(long index, byte[] buffer) {
        _bitmap.setPosition(index);
        _bitmap.write(buffer, 0, buffer.length);
        _bitmap.flush();
    }
}
