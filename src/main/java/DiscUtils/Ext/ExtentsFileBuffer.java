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

package DiscUtils.Ext;

import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Buffer.Buffer;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.IOException;


public class ExtentsFileBuffer extends Buffer {
    private final Context _context;

    private final Inode _inode;

    public ExtentsFileBuffer(Context context, Inode inode) {
        _context = context;
        _inode = inode;
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public long getCapacity() {
        return _inode.FileSize;
    }

    public int read(long pos, byte[] buffer, int offset, int count) {
        if (pos > _inode.FileSize) {
            return 0;
        }

        long blockSize = _context.getSuperBlock().getBlockSize();
        int totalRead = 0;
        int totalBytesRemaining = (int) Math.min(count, _inode.FileSize - pos);
        ExtentBlock extents = _inode.Extents;
        while (totalBytesRemaining > 0) {
            int logicalBlock = (int) ((pos + totalRead) / blockSize);
            int blockOffset = (int) (pos + totalRead - logicalBlock * blockSize);
            int numRead = 0;
            Extent extent = findExtent(extents, logicalBlock);
            if (extent == null) {
                throw new IOException("Unable to find extent for block " + logicalBlock);
            }

            if (extent.FirstLogicalBlock > logicalBlock) {
                numRead = (int) Math.min(totalBytesRemaining,
                                         (extent.FirstLogicalBlock - logicalBlock) * blockSize - blockOffset);
                Arrays.fill(buffer, offset + totalRead, numRead, (byte) 0);
            } else {
                long physicalBlock = logicalBlock - extent.FirstLogicalBlock + extent.getFirstPhysicalBlock();
                int toRead = (int) Math
                        .min(totalBytesRemaining,
                             (extent.NumBlocks - (logicalBlock - extent.FirstLogicalBlock)) * blockSize - blockOffset);
                _context.getRawStream().setPosition(physicalBlock * blockSize + blockOffset);
                numRead = _context.getRawStream().read(buffer, offset + totalRead, toRead);
            }
            totalBytesRemaining -= numRead;
            totalRead += numRead;
        }
        return totalRead;
    }

    public void write(long pos, byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    public void setCapacity(long value) {
        throw new UnsupportedOperationException();
    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
        return StreamExtent.intersect(Arrays.asList(new StreamExtent(start, count)));
    }

    private Extent findExtent(ExtentBlock node, int logicalBlock) {
        if (node.Index != null) {
            ExtentIndex idxEntry = null;
            if (node.Index.length == 0) {
                return null;
            }

            if (node.Index[0].FirstLogicalBlock >= logicalBlock) {
                idxEntry = node.Index[0];
            } else {
                for (int i = 0; i < node.Index.length; ++i) {
                    if (node.Index[i].FirstLogicalBlock > logicalBlock) {
                        idxEntry = node.Index[i - 1];
                        break;
                    }

                }
            }
            if (idxEntry == null) {
                idxEntry = node.Index[node.Index.length - 1];
            }

            ExtentBlock subBlock = loadExtentBlock(idxEntry);
            return findExtent(subBlock, logicalBlock);
        }

        if (node.Extents != null) {
            Extent entry = null;
            if (node.Extents.length == 0) {
                return null;
            }

            if (node.Extents[0].FirstLogicalBlock >= logicalBlock) {
                return node.Extents[0];
            }

            for (int i = 0; i < node.Extents.length; ++i) {
                if (node.Extents[i].FirstLogicalBlock > logicalBlock) {
                    entry = node.Extents[i - 1];
                    break;
                }

            }
            if (entry == null) {
                entry = node.Extents[node.Extents.length - 1];
            }

            return entry;
        }

        return null;
    }

    private ExtentBlock loadExtentBlock(ExtentIndex idxEntry) {
        int blockSize = _context.getSuperBlock().getBlockSize();
        _context.getRawStream().setPosition(idxEntry.getLeafPhysicalBlock() * blockSize);
        byte[] buffer = StreamUtilities.readExact(_context.getRawStream(), blockSize);
        ExtentBlock subBlock = EndianUtilities.<ExtentBlock> toStruct(ExtentBlock.class, buffer, 0);
        return subBlock;
    }
}
