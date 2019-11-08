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

package DiscUtils.Streams.Builder;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.StreamExtent;


public abstract class BuilderExtent implements Closeable {
    public BuilderExtent(long start, long length) {
        _start = start;
        _length = length;
    }

    protected long _length;

    public long getLength() {
        return _length;
    }

    protected long _start;

    public long getStart() {
        return _start;
    }

    /**
     * Gets the parts of the stream that are stored.
     * This may be an empty enumeration if all bytes are zero.
     */
    public List<StreamExtent> getStreamExtents() {
        return Arrays.asList(new StreamExtent(_start, _length));
    }

    public abstract void close() throws IOException;

    public abstract void prepareForRead();

    public abstract int read(long diskOffset, byte[] block, int offset, int count);

    public abstract void disposeReadState();

    public String toString() {
        return getClass().getSimpleName() + "@" + hashCode() + ": {start: " + _start + ", length: " + _length + "}";
    }
}
