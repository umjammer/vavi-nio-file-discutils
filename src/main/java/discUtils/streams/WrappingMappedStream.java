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

package discUtils.streams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import discUtils.streams.util.Ownership;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


/**
 * base class for streams that wrap another stream.
 * The type of stream to wrap.
 * Provides the default implementation of methods & properties, so
 * wrapping streams need only override the methods they need to intercept.
 */
public class WrappingMappedStream<T extends Stream> extends MappedStream {

    private List<StreamExtent> extents;

    private final Ownership ownership;

    public WrappingMappedStream(T toWrap, Ownership ownership, List<StreamExtent> extents) {
        wrappedStream = toWrap;
        this.ownership = ownership;
        if (extents != null) {
            this.extents = new ArrayList<>(extents);
        }
    }

    @Override public boolean canRead() {
        return wrappedStream.canRead();
    }

    @Override public boolean canSeek() {
        return wrappedStream.canSeek();
    }

    @Override public boolean canWrite() {
        return wrappedStream.canWrite();
    }

    @Override public List<StreamExtent> getExtents() {
        if (extents != null) {
            return extents;
        }
        if (wrappedStream instanceof SparseStream) {
            return ((SparseStream) wrappedStream).getExtents();
        }
        return Collections.singletonList(new StreamExtent(0, wrappedStream.getLength()));
    }

    @Override public long getLength() {
        return wrappedStream.getLength();
    }

    @Override public long position() {
        return wrappedStream.position();
    }

    @Override public void position(long value) {
        wrappedStream.position(value);
    }

    private T wrappedStream;

    protected T getWrappedStream() {
        return wrappedStream;
    }

    protected void setWrappedStream(T value) {
        wrappedStream = value;
    }

    @Override public List<StreamExtent> mapContent(long start, long length) {
        if (getWrappedStream() instanceof MappedStream) {
            return ((MappedStream) getWrappedStream()).mapContent(start, length);
        }
        return Collections.singletonList(new StreamExtent(start, length));
    }

    @Override public void flush() {
        getWrappedStream().flush();
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        return wrappedStream.read(buffer, offset, count);
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        return wrappedStream.seek(offset, origin);
    }

    @Override public void setLength(long value) {
        wrappedStream.setLength(value);
    }

    @Override public void clear(int count) {
        if (wrappedStream instanceof SparseStream) {
            ((SparseStream) wrappedStream).clear(count);
        } else {
            super.clear(count);
        }
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        wrappedStream.write(buffer, offset, count);
    }

    @Override public void close() throws IOException {
        if (wrappedStream != null && ownership == Ownership.Dispose) {
            wrappedStream.close();
        }

        wrappedStream = null;
    }
}
