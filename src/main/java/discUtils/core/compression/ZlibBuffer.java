//
// Copyright (c) 2014, Quamotion
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

package discUtils.core.compression;

import java.util.Collections;
import java.util.List;

import discUtils.streams.StreamExtent;
import discUtils.streams.buffer.Buffer;
import discUtils.streams.util.Ownership;
import dotnet4j.io.Stream;


public class ZlibBuffer extends Buffer {

    @SuppressWarnings("unused")
    private Ownership ownership;

    private final Stream stream;

    private int position;

    public ZlibBuffer(Stream stream, Ownership ownership) {
        this.stream = stream;
        this.ownership = ownership;
        position = 0;
    }

    @Override public boolean canRead() {
        return stream.canRead();
    }

    @Override public boolean canWrite() {
        return stream.canWrite();
    }

    @Override public long getCapacity() {
        return stream.getLength();
    }

    @Override public int read(long pos, byte[] buffer, int offset, int count) {
        if (pos != position) {
            throw new UnsupportedOperationException();
        }

        int read = stream.read(buffer, offset, count);
        position += read;
        return read;
    }

    @Override public void write(long pos, byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    @Override public void setCapacity(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public List<StreamExtent> getExtentsInRange(long start, long count) {
        return Collections.singletonList(new StreamExtent(0, stream.getLength()));
    }
}
