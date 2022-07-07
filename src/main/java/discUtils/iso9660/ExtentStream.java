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

package discUtils.iso9660;

import java.io.IOException;

import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class ExtentStream extends Stream {
    private final long _dataLength;

    private final byte _fileUnitSize;

    private final byte _interleaveGapSize;

    private final Stream _isoStream;

    private long _position;

    private final int _startBlock;

    public ExtentStream(Stream isoStream, int startBlock, long dataLength, byte fileUnitSize, byte interleaveGapSize) {
        _isoStream = isoStream;
        _startBlock = startBlock;
        _dataLength = dataLength;
        _fileUnitSize = fileUnitSize;
        _interleaveGapSize = interleaveGapSize;
        if (_fileUnitSize != 0 || _interleaveGapSize != 0) {
            throw new UnsupportedOperationException("Non-contiguous extents not supported");
        }
    }

    public boolean canRead() {
        return true;
    }

    public boolean canSeek() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public long getLength() {
        return _dataLength;
    }

    public long getPosition() {
        return _position;
    }

    public void setPosition(long value) {
        _position = value;
    }

    public void flush() {
    }

    public int read(byte[] buffer, int offset, int count) {
        if (_position > _dataLength) {
            return 0;
        }

        int toRead = (int) Math.min(count, _dataLength - _position);
        _isoStream.setPosition(_position + _startBlock * (long) IsoUtilities.SectorSize);
        int numRead = _isoStream.read(buffer, offset, toRead);
        _position += numRead;
        return numRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        long newPos = offset;
        if (origin == SeekOrigin.Current) {
            newPos += _position;
        } else if (origin == SeekOrigin.End) {
            newPos += _dataLength;
        }

        _position = newPos;
        return newPos;
    }

    public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        _isoStream.close();
    }
}
