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

package DiscUtils.SquashFs;

import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Block.Block;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.EndianUtilities;


public class FileContentBuffer implements IBuffer {
    private static final int InvalidFragmentKey = 0xFFFFFFFF;

    private final int[] _blockLengths;

    private final Context _context;

    private final RegularInode _inode;

    public FileContentBuffer(Context context, RegularInode inode, MetadataRef inodeRef) {
        _context = context;
        _inode = inode;
        context.getInodeReader().setPosition(inodeRef);
        context.getInodeReader().skip(_inode.size());
        int numBlocks = (int) (_inode.getFileSize() / _context.getSuperBlock().BlockSize);
        if (_inode.getFileSize() % _context.getSuperBlock().BlockSize != 0 && _inode.FragmentKey == InvalidFragmentKey) {
            ++numBlocks;
        }

        byte[] lengthData = new byte[numBlocks * 4];
        context.getInodeReader().read(lengthData, 0, lengthData.length);
        _blockLengths = new int[numBlocks];
        for (int i = 0; i < numBlocks; ++i) {
            _blockLengths[i] = EndianUtilities.toInt32LittleEndian(lengthData, i * 4);
        }
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public long getCapacity() {
        return _inode.getFileSize();
    }

    public List<StreamExtent> getExtents() {
        return Arrays.asList(new StreamExtent(0, getCapacity()));
    }

    public int read(long pos, byte[] buffer, int offset, int count) {
        if (pos > _inode.getFileSize()) {
            return 0;
        }

        long startOfFragment = _blockLengths.length * _context.getSuperBlock().BlockSize;
        long currentPos = pos;
        int totalRead = 0;
        int totalToRead = (int) Math.min(_inode.getFileSize() - pos, count);
        int currentBlock = 0;
        long currentBlockDiskStart = _inode.StartBlock;
        while (totalRead < totalToRead) {
            if (currentPos >= startOfFragment) {
                int read = readFrag((int) (currentPos - startOfFragment), buffer, offset + totalRead, totalToRead - totalRead);
                return totalRead + read;
            }

            int targetBlock = (int) (currentPos / _context.getSuperBlock().BlockSize);
            while (currentBlock < targetBlock) {
                currentBlockDiskStart += _blockLengths[currentBlock] & 0x7FFFFF;
                ++currentBlock;
            }
            int blockOffset = (int) (pos % _context.getSuperBlock().BlockSize);
            Block block = _context.getReadBlock().invoke((int) currentBlockDiskStart, _blockLengths[currentBlock]);
            int toCopy = Math.min(block.getAvailable() - blockOffset, totalToRead - totalRead);
            System.arraycopy(block.getData(), blockOffset, buffer, offset + totalRead, toCopy);
            totalRead += toCopy;
            currentPos += toCopy;
        }
        return totalRead;
    }

    public void write(long pos, byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    public void clear(long pos, int count) {
        throw new UnsupportedOperationException();
    }

    public void flush() {
    }

    public void setCapacity(long value) {
        throw new UnsupportedOperationException();
    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
        return StreamExtent.intersect(getExtents(), new StreamExtent(start, count));
    }

    private int readFrag(int pos, byte[] buffer, int offset, int count) {
        int fragRecordsPerBlock = 8192 / FragmentRecord.RecordSize;
        int fragTable = _inode.FragmentKey / fragRecordsPerBlock;
        int recordOffset = _inode.FragmentKey % fragRecordsPerBlock * FragmentRecord.RecordSize;
        byte[] fragRecordData = new byte[FragmentRecord.RecordSize];
        _context.getFragmentTableReaders()[fragTable].setPosition(0, recordOffset);
        _context.getFragmentTableReaders()[fragTable].read(fragRecordData, 0, fragRecordData.length);
        FragmentRecord fragRecord = new FragmentRecord();
        fragRecord.readFrom(fragRecordData, 0);
        Block frag = _context.getReadBlock().invoke(fragRecord.StartBlock, fragRecord.CompressedSize);
        // Attempt to read data beyond end of fragment
        if (pos > frag.getAvailable()) {
            return 0;
        }

        int toCopy = Math.min(frag.getAvailable() - (_inode.FragmentOffset + pos), count);
        System.arraycopy(frag.getData(), _inode.FragmentOffset + pos, buffer, offset, toCopy);
        return toCopy;
    }
}
