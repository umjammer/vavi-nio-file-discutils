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
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.IOException;


public class ExtentsFileBuffer extends Buffer {

    private final Context context;

    private final Inode inode;

    public ExtentsFileBuffer(Context context, Inode inode) {
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

        long blockSize = context.getSuperBlock().getBlockSize();
        int totalRead = 0;
        int totalBytesRemaining = (int) Math.min(count, inode.fileSize - pos);
        ExtentBlock extents = inode.extents;
        while (totalBytesRemaining > 0) {
            int logicalBlock = (int) ((pos + totalRead) / blockSize);
            int blockOffset = (int) (pos + totalRead - logicalBlock * blockSize);
            int numRead = 0;
            Extent extent = findExtent(extents, logicalBlock);
            if (extent == null) {
                throw new IOException("Unable to find extent for block " + logicalBlock);
            }

            if (extent.firstLogicalBlock > logicalBlock) {
                numRead = (int) Math.min(totalBytesRemaining,
                                         (extent.firstLogicalBlock - logicalBlock) * blockSize - blockOffset);
                Arrays.fill(buffer, offset + totalRead, offset + totalRead + numRead, (byte) 0);
            } else {
                long physicalBlock = logicalBlock - extent.firstLogicalBlock + extent.getFirstPhysicalBlock();
                int toRead = (int) Math
                        .min(totalBytesRemaining,
                             (extent.getNumBlocks() - (logicalBlock - extent.firstLogicalBlock)) * blockSize - blockOffset);
                context.getRawStream().position(physicalBlock * blockSize + blockOffset);
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

    private Extent findExtent(ExtentBlock node, int logicalBlock) {
        if (node.index != null) {
            ExtentIndex idxEntry = null;
            if (node.index.length == 0) {
                return null;
            }

            if (node.index[0].firstLogicalBlock >= logicalBlock) {
                idxEntry = node.index[0];
            } else {
                for (int i = 0; i < node.index.length; ++i) {
                    if (node.index[i].firstLogicalBlock > logicalBlock) {
                        idxEntry = node.index[i - 1];
                        break;
                    }

                }
            }
            if (idxEntry == null) {
                idxEntry = node.index[node.index.length - 1];
            }

            ExtentBlock subBlock = loadExtentBlock(idxEntry);
            return findExtent(subBlock, logicalBlock);
        }

        if (node.extents != null) {
            Extent entry = null;
            if (node.extents.length == 0) {
                return null;
            }

            if (node.extents[0].firstLogicalBlock >= logicalBlock) {
                return node.extents[0];
            }

            for (int i = 0; i < node.extents.length; ++i) {
                if (node.extents[i].firstLogicalBlock > logicalBlock) {
                    entry = node.extents[i - 1];
                    break;
                }

            }
            if (entry == null) {
                entry = node.extents[node.extents.length - 1];
            }

            return entry;
        }

        return null;
    }

    private ExtentBlock loadExtentBlock(ExtentIndex idxEntry) {
        int blockSize = context.getSuperBlock().getBlockSize();
        context.getRawStream().position(idxEntry.getLeafPhysicalBlock() * blockSize);
        byte[] buffer = StreamUtilities.readExact(context.getRawStream(), blockSize);
        ExtentBlock subBlock = EndianUtilities.toStruct(ExtentBlock.class, buffer, 0);
        return subBlock;
    }
}
