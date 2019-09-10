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
import java.util.List;
import java.util.stream.Collectors;

import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.SeekOrigin;
import moe.yo3explorer.dotnetio4j.Stream;


public class SubStream extends MappedStream {
    private final long _first;

    private final long _length;

    private final Ownership _ownsParent;

    private final Stream _parent;

    private long _position;

    public SubStream(Stream parent, long first, long length) {
        _parent = parent;
        _first = first;
        _length = length;
        _ownsParent = Ownership.None;
        if (_first + _length > _parent.getLength()) {
            throw new IllegalArgumentException("Substream extends beyond end of parent stream");
        }

    }

    public SubStream(Stream parent, Ownership ownsParent, long first, long length) {
        _parent = parent;
        _ownsParent = ownsParent;
        _first = first;
        _length = length;
        if (_first + _length > _parent.getLength()) {
            throw new IllegalArgumentException("Substream extends beyond end of parent stream");
        }

    }

    public boolean canRead() {
        return _parent.canRead();
    }

    public boolean canSeek() {
        return _parent.canSeek();
    }

    public boolean canWrite() {
        return _parent.canWrite();
    }

    public List<StreamExtent> getExtents() {
        if (SparseStream.class.isInstance(_parent)) {
            return offsetExtents(SparseStream.class.cast(_parent).getExtentsInRange(_first, _length));
        }

        return Arrays.asList(new StreamExtent(0, _length));
    }

    public long getLength() {
        return _length;
    }

    public long getPosition() {
        return _position;
    }

    public void setPosition(long value) {
        if (value <= _length) {
            _position = value;
        } else {
            throw new IndexOutOfBoundsException("Attempt to move beyond end of stream");
        }
    }

    public List<StreamExtent> mapContent(long start, long length) {
        return Arrays.asList(new StreamExtent(start + _first, length));
    }

    public void flush() {
//        _parent.flush();
    }

    public int read(byte[] buffer, int offset, int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("Attempt to read negative bytes");
        }

        if (_position > _length) {
            return 0;
        }

        _parent.setPosition(_first + _position);
        int numRead = _parent.read(buffer, offset, (int) Math.min(count, Math.min(_length - _position, Integer.MAX_VALUE)));
        _position += numRead;
        return numRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        long absNewPos = offset;
        if (origin == SeekOrigin.Current) {
            absNewPos += _position;
        } else if (origin == SeekOrigin.End) {
            absNewPos += _length;
        }

        if (absNewPos < 0) {
            throw new IndexOutOfBoundsException("Attempt to move before start of stream");
        }

        _position = absNewPos;
        return _position;
    }

    public void setLength(long value) {
        throw new UnsupportedOperationException("Attempt to change length of a substream");
    }

    public void write(byte[] buffer, int offset, int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("Attempt to write negative bytes");
        }

        if (_position + count > _length) {
            throw new IndexOutOfBoundsException("Attempt to write beyond end of substream");
        }

        _parent.setPosition(_first + _position);
        _parent.write(buffer, offset, count);
        _position += count;
    }

    public void close() throws IOException {
        if (_ownsParent == Ownership.Dispose) {
            _parent.close();
        }
    }

    private List<StreamExtent> offsetExtents(List<StreamExtent> src) {
        return src.stream().map(e -> new StreamExtent(e.getStart() - _first, e.getLength())).collect(Collectors.toList());
    }

}
