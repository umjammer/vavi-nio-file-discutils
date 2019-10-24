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

package DiscUtils.Core.Compression;

import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Buffer.Buffer;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.Stream;


public class ZlibBuffer extends Buffer {
    private Ownership _ownership;

    private final Stream _stream;

    private int position;

    public ZlibBuffer(Stream stream, Ownership ownership) {
        _stream = stream;
        _ownership = ownership;
        position = 0;
    }

    public boolean canRead() {
        return _stream.canRead();
    }

    public boolean canWrite() {
        return _stream.canWrite();
    }

    public long getCapacity() {
        return _stream.getLength();
    }

    public int read(long pos, byte[] buffer, int offset, int count) {
        if (pos != position) {
            throw new UnsupportedOperationException();
        }

        int read = _stream.read(buffer, offset, count);
        position += read;
        return read;
    }

    public void write(long pos, byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    public void setCapacity(long value) {
        throw new UnsupportedOperationException();
    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
        return Arrays.asList(new StreamExtent(0, _stream.getLength()));
    }

}
