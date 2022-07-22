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

package discUtils.streams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import discUtils.streams.util.Ownership;
import dotnet4j.io.SeekOrigin;


public class MirrorStream extends SparseStream {

    private final boolean canRead;

    private final boolean canSeek;

    private final boolean canWrite;

    private final long length;

    private final Ownership ownsWrapped;

    private List<SparseStream> wrapped;

    public MirrorStream(Ownership ownsWrapped, List<SparseStream> wrapped) {
        this.wrapped = new ArrayList<>(wrapped);
        this.ownsWrapped = ownsWrapped;
        canRead = this.wrapped.get(0).canRead();
        canWrite = this.wrapped.get(0).canWrite();
        canSeek = this.wrapped.get(0).canSeek();
        length = this.wrapped.get(0).getLength();
        for (SparseStream stream : this.wrapped) {
            if (stream.canRead() != canRead || stream.canWrite() != canWrite || stream.canSeek() != canSeek) {
                throw new IllegalArgumentException("All mirrored streams must have the same read/write/seek permissions");
            }

            if (stream.getLength() != length) {
                throw new IllegalArgumentException("All mirrored streams must have the same length");
            }
        }
    }

    public boolean canRead() {
        return canRead;
    }

    public boolean canSeek() {
        return canSeek;
    }

    public boolean canWrite() {
        return canWrite;
    }

    public List<StreamExtent> getExtents() {
        return wrapped.get(0).getExtents();
    }

    public long getLength() {
        return length;
    }

    public long getPosition() {
        return wrapped.get(0).getPosition();
    }

    public void setPosition(long value) {
        wrapped.get(0).setPosition(value);
    }

    public void flush() {
        wrapped.get(0).flush();
    }

    public int read(byte[] buffer, int offset, int count) {
        return wrapped.get(0).read(buffer, offset, count);
    }

    public long seek(long offset, SeekOrigin origin) {
        return wrapped.get(0).seek(offset, origin);
    }

    public void setLength(long value) {
        if (value != length) {
            throw new IllegalArgumentException("Changing the stream length is not permitted for mirrored streams");
        }
    }

    public void clear(int count) {
        long pos = wrapped.get(0).getPosition();
        if (pos + count > length) {
            throw new dotnet4j.io.IOException("Attempt to clear beyond end of mirrored stream");
        }

        for (SparseStream stream : wrapped) {
            stream.setPosition(pos);
            stream.clear(count);
        }
    }

    public void write(byte[] buffer, int offset, int count) {
        long pos = wrapped.get(0).getPosition();
        if (pos + count > length) {
            throw new dotnet4j.io.IOException("Attempt to write beyond end of mirrored stream");
        }

        for (SparseStream stream  : wrapped) {
            stream.setPosition(pos);
            stream.write(buffer, offset, count);
        }
    }

    public void close() throws IOException {
        if (ownsWrapped == Ownership.Dispose && wrapped != null) {
            for (SparseStream stream : wrapped) {
                stream.close();
            }
            wrapped = null;
        }
    }
}
