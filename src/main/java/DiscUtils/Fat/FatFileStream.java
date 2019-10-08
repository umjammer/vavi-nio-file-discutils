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

package DiscUtils.Fat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.SeekOrigin;


public class FatFileStream extends SparseStream {
    private final Directory _dir;

    private final long _dirId;

    private final ClusterStream _stream;

    private boolean didWrite;

    public FatFileStream(FatFileSystem fileSystem, Directory dir, long fileId, FileAccess access) {
        _dir = dir;
        _dirId = fileId;
        DirectoryEntry dirEntry = _dir.getEntry(_dirId);
        _stream = new ClusterStream(fileSystem, access, dirEntry.getFirstCluster(), dirEntry.getFileSize());
        _stream.FirstClusterChanged = this::firstClusterAllocatedHandler;
    }

    public boolean canRead() {
        return _stream.canRead();
    }

    public boolean canSeek() {
        return _stream.canSeek();
    }

    public boolean canWrite() {
        return _stream.canWrite();
    }

    public List<StreamExtent> getExtents() {
        return Arrays.asList(new StreamExtent(0, getLength()));
    }

    public long getLength() {
        return _stream.getLength();
    }

    public long getPosition() {
        return _stream.getPosition();
    }

    public void setPosition(long value) {
        _stream.setPosition(value);
    }

    public void close() throws IOException {
        if (_dir.getFileSystem().canWrite()) {
            long now = _dir.getFileSystem().convertFromUtc(System.currentTimeMillis());
            DirectoryEntry dirEntry = _dir.getEntry(_dirId);
            dirEntry.setLastAccessTime(now);
            if (didWrite) {
                dirEntry.setFileSize((int) _stream.getLength());
                dirEntry.setLastWriteTime(now);
            }

            _dir.updateEntry(_dirId, dirEntry);
        }
    }

    public void setLength(long value) {
        didWrite = true;
        _stream.setLength(value);
    }

    public void write(byte[] buffer, int offset, int count) {
        didWrite = true;
        _stream.write(buffer, offset, count);
    }

    public void flush() {
        _stream.flush();
    }

    public int read(byte[] buffer, int offset, int count) {
        return _stream.read(buffer, offset, count);
    }

    public long seek(long offset, SeekOrigin origin) {
        return _stream.seek(offset, origin);
    }

    private void firstClusterAllocatedHandler(int cluster) {
        DirectoryEntry dirEntry = _dir.getEntry(_dirId);
        dirEntry.setFirstCluster(cluster);
        _dir.updateEntry(_dirId, dirEntry);
    }
}
