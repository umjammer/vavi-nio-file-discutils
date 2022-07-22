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

package discUtils.iscsi;

import java.util.Collections;
import java.util.List;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.MathUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.IOException;
import dotnet4j.io.SeekOrigin;


public class DiskStream extends SparseStream {
    private final int blockSize;

    private final long length;

    private final long lun;

    private long position;

    private final Session session;

    public DiskStream(Session session, long lun, FileAccess access) {
        this.session = session;
        this.lun = lun;
        LunCapacity capacity = session.getCapacity(lun);
        blockSize = capacity.getBlockSize();
        length = capacity.getLogicalBlockCount() * capacity.getBlockSize();
        canWrite = access != FileAccess.Read;
        canRead = access != FileAccess.Write;
    }

    private boolean canRead;

    public boolean canRead() {
        return canRead;
    }

    public boolean canSeek() {
        return true;
    }

    private boolean canWrite;

    public boolean canWrite() {
        return canWrite;
    }

    public List<StreamExtent> getExtents() {
        return Collections.singletonList(new StreamExtent(0, length));
    }

    public long getLength() {
        return length;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long value) {
        position = value;
    }

    public void flush() {
    }

    public int read(byte[] buffer, int offset, int count) {
        if (!canRead()) {
            throw new UnsupportedOperationException("Attempt to read from read-only stream");
        }

        int maxToRead = (int) Math.min(length - position, count);
        long firstBlock = position / blockSize;
        long lastBlock = MathUtilities.ceil(position + maxToRead, blockSize);
        byte[] tempBuffer = new byte[(int) (lastBlock - firstBlock) * blockSize];
        int numRead = session.read(lun, firstBlock, (short) (lastBlock - firstBlock), tempBuffer, 0);
        int numCopied = Math.min(maxToRead, numRead);
        System.arraycopy(tempBuffer, (int) (position - firstBlock * blockSize), buffer, offset, numCopied);
        position += numCopied;
        return numCopied;
    }

    public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += length;
        }

        if (effectiveOffset < 0) {
            throw new IOException("Attempt to move before beginning of disk");
        }

        position = effectiveOffset;
        return position;
    }

    public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        if (!canWrite()) {
            throw new IOException("Attempt to write to read-only stream");
        }

        if (position + count > length) {
            throw new IOException("Attempt to write beyond end of stream");
        }

        int numWritten = 0;
        while (numWritten < count) {
            long block = position / blockSize;
            int offsetInBlock = (int) (position % blockSize);
            int toWrite = count - numWritten;
            // Need to read - we're not handling a full block
            if (offsetInBlock != 0 || toWrite < blockSize) {
                toWrite = Math.min(toWrite, blockSize - offsetInBlock);
                byte[] blockBuffer = new byte[blockSize];
                int numRead = session.read(lun, block, (short) 1, blockBuffer, 0);
                if (numRead != blockSize) {
                    throw new IOException("Incomplete read, received " + numRead + " bytes from 1 block");
                }

                // Overlay as much data as we have for this block
                System.arraycopy(buffer, offset + numWritten, blockBuffer, offsetInBlock, toWrite);
                // Write the block back
                session.write(lun, block, (short) 1, blockSize, blockBuffer, 0);
            } else {
                // Processing at least one whole block, just write (after making sure to trim any partial sectors from the end)...
                short numBlocks = (short) (toWrite / blockSize);
                toWrite = numBlocks * blockSize;
                session.write(lun, block, numBlocks, blockSize, buffer, offset + numWritten);
            }
            numWritten += toWrite;
            position += toWrite;
        }
    }

    @Override
    public void close() throws java.io.IOException {
        session.close();
    }
}
