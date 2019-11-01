//
// Copyright (c) 2008-2012, Kenneth Bell
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


/**
 * Base class for streams that wrap another stream.
 * The type of stream to wrap.
 * Provides the default implementation of methods & properties, so
 * wrapping streams need only override the methods they need to intercept.
 */
public class WrappingMappedStream<T extends Stream> extends MappedStream {
    private List<StreamExtent> _extents;

    private final Ownership _ownership;

    public WrappingMappedStream(T toWrap, Ownership ownership, List<StreamExtent> extents) {
        setWrappedStream(toWrap);
        _ownership = ownership;
        if (extents != null) {
            _extents = new ArrayList<>(extents);
        }

    }

    public boolean canRead() {
        return getWrappedStream().canRead();
    }

    public boolean canSeek() {
        return getWrappedStream().canSeek();
    }

    public boolean canWrite() {
        return getWrappedStream().canWrite();
    }

    public List<StreamExtent> getExtents() {
        if (_extents != null) {
            return _extents;
        }
        if (SparseStream.class.isInstance(getWrappedStream())) {
            return SparseStream.class.cast(getWrappedStream()).getExtents();
        }
        return Arrays.asList(new StreamExtent(0, getWrappedStream().getLength()));
    }

    public long getLength() {
        return getWrappedStream().getLength();
    }

    public long getPosition() {
        return getWrappedStream().getPosition();
    }

    public void setPosition(long value) {
        getWrappedStream().setPosition(value);
    }

    private T __WrappedStream;

    protected T getWrappedStream() {
        return __WrappedStream;
    }

    protected void setWrappedStream(T value) {
        __WrappedStream = value;
    }

    public List<StreamExtent> mapContent(long start, long length) {
        if (MappedStream.class.isInstance(getWrappedStream())) {
            return MappedStream.class.cast(getWrappedStream()).mapContent(start, length);
        }
        return Arrays.asList(new StreamExtent(start, length));
    }

    public void flush() {
        getWrappedStream().flush();
    }

    public int read(byte[] buffer, int offset, int count) {
        return getWrappedStream().read(buffer, offset, count);
    }

    public long seek(long offset, SeekOrigin origin) {
        return getWrappedStream().seek(offset, origin);
    }

    public void setLength(long value) {
        getWrappedStream().setLength(value);
    }

    public void clear(int count) {
        if (SparseStream.class.isInstance(getWrappedStream())) {
            SparseStream.class.cast(getWrappedStream()).clear(count);
        } else {
            super.clear(count);
        }
    }

    public void write(byte[] buffer, int offset, int count) {
        getWrappedStream().write(buffer, offset, count);
    }

    public void close() throws IOException {
        if (getWrappedStream() != null && _ownership == Ownership.Dispose) {
            getWrappedStream().close();
        }

        setWrappedStream(null);
    }
}
