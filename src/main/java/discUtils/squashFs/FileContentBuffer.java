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

package discUtils.squashFs;

import java.util.Collections;
import java.util.List;

import discUtils.streams.StreamExtent;
import discUtils.streams.block.Block;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.EndianUtilities;


public class FileContentBuffer implements IBuffer {

    private static final int InvalidFragmentKey = 0xFFFFFFFF;

    private final int[] blockLengths;

    private final Context context;

    private final RegularInode inode;

    public FileContentBuffer(Context context, RegularInode inode, MetadataRef inodeRef) {
        this.context = context;
        this.inode = inode;
        context.getInodeReader().setPosition(inodeRef);
        context.getInodeReader().skip(this.inode.size());
        int numBlocks = (int) (this.inode.getFileSize() / this.context.getSuperBlock().blockSize);
        if (this.inode.getFileSize() % this.context.getSuperBlock().blockSize != 0 && this.inode.fragmentKey == InvalidFragmentKey) {
            ++numBlocks;
        }

        byte[] lengthData = new byte[numBlocks * 4];
        context.getInodeReader().read(lengthData, 0, lengthData.length);
        blockLengths = new int[numBlocks];
        for (int i = 0; i < numBlocks; ++i) {
            blockLengths[i] = EndianUtilities.toInt32LittleEndian(lengthData, i * 4);
        }
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public long getCapacity() {
        return inode.getFileSize();
    }

    public List<StreamExtent> getExtents() {
        return Collections.singletonList(new StreamExtent(0, getCapacity()));
    }

    public int read(long pos, byte[] buffer, int offset, int count) {
        if (pos > inode.getFileSize()) {
            return 0;
        }

        long startOfFragment = (long) blockLengths.length * context.getSuperBlock().blockSize;
        long currentPos = pos;
        int totalRead = 0;
        int totalToRead = (int) Math.min(inode.getFileSize() - pos, count);
        int currentBlock = 0;
        long currentBlockDiskStart = inode.startBlock;
        while (totalRead < totalToRead) {
            if (currentPos >= startOfFragment) {
                int read = readFrag((int) (currentPos - startOfFragment), buffer, offset + totalRead, totalToRead - totalRead);
                return totalRead + read;
            }

            int targetBlock = (int) (currentPos / context.getSuperBlock().blockSize);
            while (currentBlock < targetBlock) {
                currentBlockDiskStart += blockLengths[currentBlock] & 0x7FFFFF;
                ++currentBlock;
            }
            int blockOffset = (int) (pos % context.getSuperBlock().blockSize);
            Block block = context.getReadBlock().invoke((int) currentBlockDiskStart, blockLengths[currentBlock]);
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
        int fragTable = inode.fragmentKey / fragRecordsPerBlock;
        int recordOffset = inode.fragmentKey % fragRecordsPerBlock * FragmentRecord.RecordSize;
        byte[] fragRecordData = new byte[FragmentRecord.RecordSize];
        context.getFragmentTableReaders()[fragTable].setPosition(0, recordOffset);
        context.getFragmentTableReaders()[fragTable].read(fragRecordData, 0, fragRecordData.length);
        FragmentRecord fragRecord = new FragmentRecord();
        fragRecord.readFrom(fragRecordData, 0);
        Block frag = context.getReadBlock().invoke(fragRecord.startBlock, fragRecord.compressedSize);
        // Attempt to read data beyond end of fragment
        if (pos > frag.getAvailable()) {
            return 0;
        }

        int toCopy = Math.min(frag.getAvailable() - (inode.fragmentOffset + pos), count);
        System.arraycopy(frag.getData(), inode.fragmentOffset + pos, buffer, offset, toCopy);
        return toCopy;
    }
}
