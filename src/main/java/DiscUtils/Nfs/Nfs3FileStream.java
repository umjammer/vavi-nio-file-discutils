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

package DiscUtils.Nfs;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.SeekOrigin;


public final class Nfs3FileStream extends SparseStream {
    private final FileAccess _access;

    private final Nfs3Client _client;

    private final Nfs3FileHandle _handle;

    private long _length;

    private long _position;

    public Nfs3FileStream(Nfs3Client client, Nfs3FileHandle handle, FileAccess access) {
        _client = client;
        _handle = handle;
        _access = access;
        _length = _client.getAttributes(_handle).Size;
    }

    public boolean canRead() {
        return _access != FileAccess.Write;
    }

    public boolean canSeek() {
        return true;
    }

    public boolean canWrite() {
        return _access != FileAccess.Read;
    }

    public List<StreamExtent> getExtents() {
        return Arrays.asList(new StreamExtent(0, getLength()));
    }

    public long getLength() {
        return _length;
    }

    public long getPosition() {
        return _position;
    }

    public void setPosition(long value) {
        _position = value;
    }

    public void flush() {
    }

    public int read(byte[] buffer, int offset, int count) {
        int numToRead = Math.min(_client.getFileSystemInfo().getReadMaxBytes(), count);
        Nfs3ReadResult readResult = _client.read(_handle, _position, numToRead);
        int toCopy = Math.min(count, readResult.getCount());
        System.arraycopy(readResult.getData(), 0, buffer, offset, toCopy);
        if (readResult.getEof()) {
            _length = _position + readResult.getCount();
        }

        _position += toCopy;
        return toCopy;
    }

    public long seek(long offset, SeekOrigin origin) {
        long newPos = offset;
        if (origin == SeekOrigin.Current) {
            newPos += _position;
        } else if (origin == SeekOrigin.End) {
            newPos += getLength();
        }

        _position = newPos;
        return newPos;
    }

    public void setLength(long value) {
        if (canWrite()) {
            _client.setAttributes(_handle, new Nfs3SetAttributes());
            _length = value;
        } else {
            throw new UnsupportedOperationException("Attempt to change length of read-only file");
        }
    }

    public void write(byte[] buffer, int offset, int count) {
        int totalWritten = 0;
        while (totalWritten < count) {
            int numToWrite = Math.min(_client.getFileSystemInfo().getWriteMaxBytes(), count - totalWritten);
            int numWritten = _client.write(_handle, _position, buffer, offset + totalWritten, numToWrite);
            _position += numWritten;
            totalWritten += numWritten;
        }
        _length = Math.max(_length, _position);
    }

    @Override
    public void close() throws IOException {
        _client.close();
    }
}
