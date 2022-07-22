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

package discUtils.fat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import dotnet4j.io.FileAccess;
import dotnet4j.io.SeekOrigin;


public class FatFileStream extends SparseStream {

    private final Directory dir;

    private final long dirId;

    private final ClusterStream stream;

    private boolean didWrite;

    public FatFileStream(FatFileSystem fileSystem, Directory dir, long fileId, FileAccess access) {
        this.dir = dir;
        dirId = fileId;
        DirectoryEntry dirEntry = this.dir.getEntry(dirId);
        stream = new ClusterStream(fileSystem, access, dirEntry.getFirstCluster(), dirEntry.getFileSize());
        stream.firstClusterChanged = this::firstClusterAllocatedHandler;
    }

    public boolean canRead() {
        return stream.canRead();
    }

    public boolean canSeek() {
        return stream.canSeek();
    }

    public boolean canWrite() {
        return stream.canWrite();
    }

    public List<StreamExtent> getExtents() {
        return Collections.singletonList(new StreamExtent(0, getLength()));
    }

    public long getLength() {
        return stream.getLength();
    }

    public long getPosition() {
        return stream.getPosition();
    }

    public void setPosition(long value) {
        stream.setPosition(value);
    }

    public void close() throws IOException {
        if (dir.getFileSystem().canWrite()) {
            long now = dir.getFileSystem().convertFromUtc(System.currentTimeMillis());
            DirectoryEntry dirEntry = dir.getEntry(dirId);
            dirEntry.setLastAccessTime(now);
            if (didWrite) {
                dirEntry.setFileSize((int) stream.getLength());
                dirEntry.setLastWriteTime(now);
            }

            dir.updateEntry(dirId, dirEntry);
        }
    }

    public void setLength(long value) {
        didWrite = true;
        stream.setLength(value);
    }

    public void write(byte[] buffer, int offset, int count) {
        didWrite = true;
        stream.write(buffer, offset, count);
    }

    public void flush() {
        stream.flush();
    }

    public int read(byte[] buffer, int offset, int count) {
        return stream.read(buffer, offset, count);
    }

    public long seek(long offset, SeekOrigin origin) {
        return stream.seek(offset, origin);
    }

    private void firstClusterAllocatedHandler(int cluster) {
        DirectoryEntry dirEntry = dir.getEntry(dirId);
        dirEntry.setFirstCluster(cluster);
        dir.updateEntry(dirId, dirEntry);
    }
}
