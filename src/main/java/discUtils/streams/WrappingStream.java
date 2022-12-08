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
import java.util.List;

import discUtils.streams.util.Ownership;
import dotnet4j.io.SeekOrigin;


/**
 * base class for streams that wrap another stream.
 *
 * Provides the default implementation of methods & properties, so
 * wrapping streams need only override the methods they need to intercept.
 */
public class WrappingStream extends SparseStream {

    private final Ownership ownership;

    private SparseStream wrapped;

    public WrappingStream(SparseStream toWrap, Ownership ownership) {
        wrapped = toWrap;
        this.ownership = ownership;
    }

    @Override public boolean canRead() {
        return wrapped.canRead();
    }

    @Override public boolean canSeek() {
        return wrapped.canSeek();
    }

    @Override public boolean canWrite() {
        return wrapped.canWrite();
    }

    @Override public List<StreamExtent> getExtents() {
        return wrapped.getExtents();
    }

    @Override public long getLength() {
        return wrapped.getLength();
    }

    @Override public long position() {
        return wrapped.position();
    }

    @Override public void position(long value) {
        wrapped.position(value);
    }

    @Override public void flush() {
        wrapped.flush();
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        return wrapped.read(buffer, offset, count);
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        return wrapped.seek(offset, origin);
    }

    @Override public void setLength(long value) {
        wrapped.setLength(value);
    }

    @Override public void clear(int count) {
        wrapped.clear(count);
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        wrapped.write(buffer, offset, count);
    }

    @Override public void close() throws IOException {
        if (wrapped != null && ownership == Ownership.Dispose) {
            wrapped.close();
        }

        wrapped = null;
    }
}
