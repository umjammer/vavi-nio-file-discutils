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

package discUtils.nfs;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import dotnet4j.io.FileAccess;
import dotnet4j.io.SeekOrigin;


public final class Nfs3FileStream extends SparseStream {

    private final FileAccess access;

    private final Nfs3Client client;

    private final Nfs3FileHandle handle;

    private long length;

    private long position;

    public Nfs3FileStream(Nfs3Client client, Nfs3FileHandle handle, FileAccess access) {
        this.client = client;
        this.handle = handle;
        this.access = access;
        length = this.client.getAttributes(this.handle).size;
    }

    @Override public boolean canRead() {
        return access != FileAccess.Write;
    }

    @Override public boolean canSeek() {
        return true;
    }

    @Override public boolean canWrite() {
        return access != FileAccess.Read;
    }

    @Override public List<StreamExtent> getExtents() {
        return Collections.singletonList(new StreamExtent(0, getLength()));
    }

    @Override public long getLength() {
        return length;
    }

    @Override public long position() {
        return position;
    }

    @Override public void position(long value) {
        position = value;
    }

    @Override public void flush() {
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        int numToRead = Math.min(client.getFileSystemInfo().getReadMaxBytes(), count);
        Nfs3ReadResult readResult = client.read(handle, position, numToRead);
        int toCopy = Math.min(count, readResult.getCount());
        System.arraycopy(readResult.getData(), 0, buffer, offset, toCopy);
        if (readResult.getEof()) {
            length = position + readResult.getCount();
        }

        position += toCopy;
        return toCopy;
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        long newPos = offset;
        if (origin == SeekOrigin.Current) {
            newPos += position;
        } else if (origin == SeekOrigin.End) {
            newPos += getLength();
        }

        position = newPos;
        return newPos;
    }

    @Override public void setLength(long value) {
        if (canWrite()) {
            client.setAttributes(handle, new Nfs3SetAttributes());
            length = value;
        } else {
            throw new UnsupportedOperationException("Attempt to change length of read-only file");
        }
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        int totalWritten = 0;
        while (totalWritten < count) {
            int numToWrite = Math.min(client.getFileSystemInfo().getWriteMaxBytes(), count - totalWritten);
            int numWritten = client.write(handle, position, buffer, offset + totalWritten, numToWrite);
            position += numWritten;
            totalWritten += numWritten;
        }
        length = Math.max(length, position);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
