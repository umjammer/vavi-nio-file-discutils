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

package DiscUtils.Streams;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dotnet4j.io.SeekOrigin;


/**
 * A stream that returns Zero's.
 */
public class ZeroStream extends MappedStream {
    private boolean _atEof;

    private final long _length;

    private long _position;

    public ZeroStream(long length) {
        _length = length;
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

    // The stream is entirely sparse
    public List<StreamExtent> getExtents() {
        return Collections.EMPTY_LIST;
    }

    public long getLength() {
        return _length;
    }

    public long getPosition() {
        return _position;
    }

    public void setPosition(long value) {
        _position = value;
        _atEof = false;
    }

    public List<StreamExtent> mapContent(long start, long length) {
        return Arrays.asList();
    }

    public void flush() {
    }

    public int read(byte[] buffer, int offset, int count) {
        if (_position > _length) {
            _atEof = true;
            throw new dotnet4j.io.IOException("Attempt to read beyond end of stream");
        }

        if (_position == _length) {
            if (_atEof) {
                throw new dotnet4j.io.IOException("Attempt to read beyond end of stream");
            }

            _atEof = true;
            return 0;
        }

        int numToClear = (int) Math.min(count, _length - _position);
        Arrays.fill(buffer, offset, offset + numToClear, (byte) 0);
        _position += numToClear;
        return numToClear;
    }

    public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += _position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += _length;
        }

        _atEof = false;
        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of stream");
        }

        _position = effectiveOffset;
        return _position;
    }

    public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
    }
}
