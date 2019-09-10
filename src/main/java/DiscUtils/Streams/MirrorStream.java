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
import java.util.ArrayList;
import java.util.List;

import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.SeekOrigin;


public class MirrorStream extends SparseStream {
    private final boolean _canRead;

    private final boolean _canSeek;

    private final boolean _canWrite;

    private final long _length;

    private final Ownership _ownsWrapped;

    private List<SparseStream> _wrapped;

    public MirrorStream(Ownership ownsWrapped, List<SparseStream> wrapped) {
        _wrapped = new ArrayList<>(wrapped);
        _ownsWrapped = ownsWrapped;
        _canRead = _wrapped.get(0).canRead();
        _canWrite = _wrapped.get(0).canWrite();
        _canSeek = _wrapped.get(0).canSeek();
        _length = _wrapped.get(0).getLength();
        for (Object __dummyForeachVar0 : _wrapped) {
            SparseStream stream = (SparseStream) __dummyForeachVar0;
            if (stream.canRead() != _canRead || stream.canWrite() != _canWrite || stream.canSeek() != _canSeek) {
                throw new IllegalArgumentException("All mirrored streams must have the same read/write/seek permissions");
            }

            if (stream.getLength() != _length) {
                throw new IllegalArgumentException("All mirrored streams must have the same length");
            }

        }
    }

    public boolean canRead() {
        return _canRead;
    }

    public boolean canSeek() {
        return _canSeek;
    }

    public boolean canWrite() {
        return _canWrite;
    }

    public List<StreamExtent> getExtents() {
        return _wrapped.get(0).getExtents();
    }

    public long getLength() {
        return _length;
    }

    public long getPosition() {
        return _wrapped.get(0).getPosition();
    }

    public void setPosition(long value) {
        _wrapped.get(0).setPosition(value);
    }

    public void flush() {
//        _wrapped.get(0).flush();
    }

    public int read(byte[] buffer, int offset, int count) {
        return _wrapped.get(0).read(buffer, offset, count);
    }

    public long seek(long offset, SeekOrigin origin) {
        return _wrapped.get(0).seek(offset, origin);
    }

    public void setLength(long value) {
        if (value != _length) {
            throw new IllegalArgumentException("Changing the stream length is not permitted for mirrored streams");
        }

    }

    public void clear(int count) {
        long pos = _wrapped.get(0).getPosition();
        if (pos + count > _length) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to clear beyond end of mirrored stream");
        }

        for (Object __dummyForeachVar1 : _wrapped) {
            SparseStream stream = (SparseStream) __dummyForeachVar1;
            stream.setPosition(pos);
            stream.clear(count);
        }
    }

    public void write(byte[] buffer, int offset, int count) {
        long pos = _wrapped.get(0).getPosition();
        if (pos + count > _length) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to write beyond end of mirrored stream");
        }

        for (Object __dummyForeachVar2 : _wrapped) {
            SparseStream stream = (SparseStream) __dummyForeachVar2;
            stream.setPosition(pos);
            stream.write(buffer, offset, count);
        }
    }

    public void close() throws IOException {
        if (_ownsWrapped == Ownership.Dispose && _wrapped != null) {
            for (Object __dummyForeachVar3 : _wrapped) {
                SparseStream stream = (SparseStream) __dummyForeachVar3;
                stream.close();
            }
            _wrapped = null;
        }
    }

}
