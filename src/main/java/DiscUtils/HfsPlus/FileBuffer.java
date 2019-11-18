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

package DiscUtils.HfsPlus;

import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Buffer.Buffer;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


final class FileBuffer extends Buffer {
    private final ForkData _baseData;

    private final CatalogNodeId _cnid;

    private final Context _context;

    public FileBuffer(Context context, ForkData baseData, CatalogNodeId catalogNodeId) {
        _context = context;
        _baseData = baseData;
        _cnid = catalogNodeId;
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public long getCapacity() {
        return _baseData.LogicalSize;
    }

    public int read(long pos, byte[] buffer, int offset, int count) {
        int totalRead = 0;

        int limitedCount = (int) Math.min(count, Math.max(0, getCapacity() - pos));

        while (totalRead < limitedCount) {
            long[] extentLogicalStart = new long[1];
            ExtentDescriptor extent = findExtent(pos, extentLogicalStart);
            long extentStreamStart = extent.StartBlock * (long) _context.getVolumeHeader().BlockSize;
            long extentSize = extent.BlockCount * (long) _context.getVolumeHeader().BlockSize;

            long extentOffset = pos + totalRead - extentLogicalStart[0];
            int toRead = (int) Math.min(limitedCount - totalRead, extentSize - extentOffset);

            // Remaining in extent can create a situation where amount to read is zero, and that appears
            // to be OK, just need to exit this while loop to avoid infinite loop.
            if (toRead == 0) {
                break;
            }

            Stream volStream = _context.getVolumeStream();
            volStream.setPosition(extentStreamStart + extentOffset);
            int numRead = volStream.read(buffer, offset + totalRead, toRead);

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
        return Arrays.asList(new StreamExtent(start, Math.min(start + count, getCapacity()) - start));
    }

    /**
     * @param extentLogicalStart {@cs out}
     */
    private ExtentDescriptor findExtent(long pos, long[] extentLogicalStart) {
        int blocksSeen = 0;
        int block = (int) (pos / _context.getVolumeHeader().BlockSize);
        for (int i = 0; i < _baseData.Extents.length; ++i) {
            if (blocksSeen + _baseData.Extents[i].BlockCount > block) {
                extentLogicalStart[0] = blocksSeen * (long) _context.getVolumeHeader().BlockSize;
                return _baseData.Extents[i];
            }

            blocksSeen += _baseData.Extents[i].BlockCount;
        }

        while (blocksSeen < _baseData.TotalBlocks) {
            byte[] extentData = _context.getExtentsOverflow().find(new ExtentKey(_cnid, blocksSeen, false));

            if (extentData != null) {
                int extentDescriptorCount = extentData.length / 8;
                for (int a = 0; a < extentDescriptorCount; a++) {
                    ExtentDescriptor extentDescriptor = new ExtentDescriptor();
                    @SuppressWarnings("unused")
                    int bytesRead = extentDescriptor.readFrom(extentData, a * 8);

                    if (blocksSeen + extentDescriptor.BlockCount > block) {
                        extentLogicalStart[0] = blocksSeen * (long) _context.getVolumeHeader().BlockSize;
                        return extentDescriptor;
                    }

                    blocksSeen += extentDescriptor.BlockCount;
                }
            } else {
                throw new IOException("Missing extent from extent overflow file: cnid=" + _cnid + ", blocksSeen=" + blocksSeen);
            }
        }

        throw new UnsupportedOperationException("Requested file fragment beyond EOF");
    }
}
