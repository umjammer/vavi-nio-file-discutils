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

package DiscUtils.Iscsi;

import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Util.MathUtilities;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.SeekOrigin;


public class DiskStream extends SparseStream {
    private final int _blockSize;

    private final long _length;

    private final long _lun;

    private long _position;

    private final Session _session;

    public DiskStream(Session session, long lun, FileAccess access) {
        _session = session;
        _lun = lun;
        LunCapacity capacity = session.getCapacity(lun);
        _blockSize = capacity.getBlockSize();
        _length = capacity.getLogicalBlockCount() * capacity.getBlockSize();
        __CanWrite = access != FileAccess.Read;
        __CanRead = access != FileAccess.Write;
    }

    private boolean __CanRead;

    public boolean canRead() {
        return __CanRead;
    }

    public boolean canSeek() {
        return true;
    }

    private boolean __CanWrite;

    public boolean canWrite() {
        return __CanWrite;
    }

    public List<StreamExtent> getExtents() {
        return Arrays.asList(new StreamExtent(0, _length));
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
        if (!canRead()) {
            throw new UnsupportedOperationException("Attempt to read from read-only stream");
        }

        int maxToRead = (int) Math.min(_length - _position, count);
        long firstBlock = _position / _blockSize;
        long lastBlock = MathUtilities.ceil(_position + maxToRead, _blockSize);
        byte[] tempBuffer = new byte[(int) (lastBlock - firstBlock) * _blockSize];
        int numRead = _session.read(_lun, firstBlock, (short) (lastBlock - firstBlock), tempBuffer, 0);
        int numCopied = Math.min(maxToRead, numRead);
        System.arraycopy(tempBuffer, (int) (_position - firstBlock * _blockSize), buffer, offset, numCopied);
        _position += numCopied;
        return numCopied;
    }

    public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += _position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += _length;
        }

        if (effectiveOffset < 0) {
            throw new IOException("Attempt to move before beginning of disk");
        }

        _position = effectiveOffset;
        return _position;
    }

    public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        if (!canWrite()) {
            throw new IOException("Attempt to write to read-only stream");
        }

        if (_position + count > _length) {
            throw new IOException("Attempt to write beyond end of stream");
        }

        int numWritten = 0;
        while (numWritten < count) {
            long block = _position / _blockSize;
            int offsetInBlock = (int) (_position % _blockSize);
            int toWrite = count - numWritten;
            // Need to read - we're not handling a full block
            if (offsetInBlock != 0 || toWrite < _blockSize) {
                toWrite = Math.min(toWrite, _blockSize - offsetInBlock);
                byte[] blockBuffer = new byte[_blockSize];
                int numRead = _session.read(_lun, block, (short) 1, blockBuffer, 0);
                if (numRead != _blockSize) {
                    throw new IOException("Incomplete read, received " + numRead + " bytes from 1 block");
                }

                // Overlay as much data as we have for this block
                System.arraycopy(buffer, offset + numWritten, blockBuffer, offsetInBlock, toWrite);
                // Write the block back
                _session.write(_lun, block, (short) 1, _blockSize, blockBuffer, 0);
            } else {
                // Processing at least one whole block, just write (after making sure to trim any partial sectors from the end)...
                short numBlocks = (short) (toWrite / _blockSize);
                toWrite = numBlocks * _blockSize;
                _session.write(_lun, block, numBlocks, _blockSize, buffer, offset + numWritten);
            }
            numWritten += toWrite;
            _position += toWrite;
        }
    }

    @Override
    public void close() throws java.io.IOException {
        _session.close();
    }
}
