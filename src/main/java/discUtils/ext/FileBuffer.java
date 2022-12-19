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

package discUtils.ext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import discUtils.streams.StreamExtent;
import discUtils.streams.buffer.Buffer;
import discUtils.streams.util.StreamUtilities;
import vavi.util.ByteUtil;


public class FileBuffer extends Buffer {

    private final Context context;

    private final Inode inode;

    public FileBuffer(Context context, Inode inode) {
        this.context = context;
        this.inode = inode;
    }

    @Override public boolean canRead() {
        return true;
    }

    @Override public boolean canWrite() {
        return false;
    }

    @Override public long getCapacity() {
        return inode.fileSize;
    }

    @Override public int read(long pos, byte[] buffer, int offset, int count) {
        if (pos > inode.fileSize) {
            return 0;
        }

        int blockSize = context.getSuperBlock().getBlockSize();
        int totalRead = 0;
        int totalBytesRemaining = (int) Math.min(count, inode.fileSize - pos);
        while (totalBytesRemaining > 0) {
            int logicalBlock = (int) ((pos + totalRead) / blockSize);
            int blockOffset = (int) (pos + totalRead - logicalBlock * (long) blockSize);
            int physicalBlock = 0;
            if (logicalBlock < 12) {
                physicalBlock = inode.directBlocks[logicalBlock];
            } else {
                logicalBlock -= 12;
                if (logicalBlock < blockSize / 4) {
                    if (inode.indirectBlock != 0) {
                        context.getRawStream().position(inode.indirectBlock * (long) blockSize + logicalBlock * 4);
                        byte[] indirectData = StreamUtilities.readExact(context.getRawStream(), 4);
                        physicalBlock = ByteUtil.readLeInt(indirectData, 0);
                    }

                } else {
                    logicalBlock -= blockSize / 4;
                    if (logicalBlock < blockSize / 4 * (blockSize / 4)) {
                        if (inode.doubleIndirectBlock != 0) {
                            context.getRawStream()
                                    .position(inode.doubleIndirectBlock * (long) blockSize +
                                                 logicalBlock / (blockSize / 4) * 4L);
                            byte[] indirectData = StreamUtilities.readExact(context.getRawStream(), 4);
                            int indirectBlock = ByteUtil.readLeInt(indirectData, 0);
                            if (indirectBlock != 0) {
                                context.getRawStream()
                                        .position(indirectBlock * (long) blockSize + logicalBlock % (blockSize / 4) * 4);
                                StreamUtilities.readExact(context.getRawStream(), indirectData, 0, 4);
                                physicalBlock = ByteUtil.readLeInt(indirectData, 0);
                            }

                        }

                    } else {
                        throw new UnsupportedOperationException("Triple indirection");
                    }
                }
            }
            int toRead = Math.min(totalBytesRemaining, blockSize - blockOffset);
            int numRead;
            if (physicalBlock == 0) {
                Arrays.fill(buffer, offset + totalRead, offset + totalRead + toRead, (byte) 0);
                numRead = toRead;
            } else {
                context.getRawStream().position(physicalBlock * (long) blockSize + blockOffset);
                numRead = context.getRawStream().read(buffer, offset + totalRead, toRead);
            }
            totalBytesRemaining -= numRead;
            totalRead += numRead;
        }
        return totalRead;
    }

    @Override public void write(long pos, byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    @Override public void setCapacity(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public List<StreamExtent> getExtentsInRange(long start, long count) {
        return StreamExtent.intersect(Collections.singletonList(new StreamExtent(0, getCapacity())), new StreamExtent(start, count));
    }
}
