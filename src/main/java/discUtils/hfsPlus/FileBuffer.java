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

package discUtils.hfsPlus;

import java.util.Collections;
import java.util.List;

import discUtils.streams.StreamExtent;
import discUtils.streams.buffer.Buffer;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


final class FileBuffer extends Buffer {

    private final ForkData baseData;

    private final CatalogNodeId cnid;

    private final Context context;

    public FileBuffer(Context context, ForkData baseData, CatalogNodeId catalogNodeId) {
        this.context = context;
        this.baseData = baseData;
        cnid = catalogNodeId;
    }

    @Override public boolean canRead() {
        return true;
    }

    @Override public boolean canWrite() {
        return false;
    }

    @Override public long getCapacity() {
        return baseData.logicalSize;
    }

    @Override public int read(long pos, byte[] buffer, int offset, int count) {
        int totalRead = 0;

        int limitedCount = (int) Math.min(count, Math.max(0, getCapacity() - pos));

        while (totalRead < limitedCount) {
            long[] extentLogicalStart = new long[1];
            ExtentDescriptor extent = findExtent(pos, extentLogicalStart);
            long extentStreamStart = extent.startBlock * (long) context.getVolumeHeader().blockSize;
            long extentSize = extent.blockCount * (long) context.getVolumeHeader().blockSize;

            long extentOffset = pos + totalRead - extentLogicalStart[0];
            int toRead = (int) Math.min(limitedCount - totalRead, extentSize - extentOffset);

            // Remaining in extent can create a situation where amount to read is zero, and that appears
            // to be OK, just need to exit this while loop to avoid infinite loop.
            if (toRead == 0) {
                break;
            }

            Stream volStream = context.getVolumeStream();
            volStream.position(extentStreamStart + extentOffset);
            int numRead = volStream.read(buffer, offset + totalRead, toRead);

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
        return Collections.singletonList(new StreamExtent(start, Math.min(start + count, getCapacity()) - start));
    }

    /**
     * @param extentLogicalStart {@cs out}
     */
    private ExtentDescriptor findExtent(long pos, long[] extentLogicalStart) {
        int blocksSeen = 0;
        int block = (int) (pos / context.getVolumeHeader().blockSize);
        for (int i = 0; i < baseData.extents.length; ++i) {
            if (blocksSeen + baseData.extents[i].blockCount > block) {
                extentLogicalStart[0] = blocksSeen * (long) context.getVolumeHeader().blockSize;
                return baseData.extents[i];
            }

            blocksSeen += baseData.extents[i].blockCount;
        }

        while (blocksSeen < baseData.totalBlocks) {
            byte[] extentData = context.getExtentsOverflow().find(new ExtentKey(cnid, blocksSeen, false));

            if (extentData != null) {
                int extentDescriptorCount = extentData.length / 8;
                for (int a = 0; a < extentDescriptorCount; a++) {
                    ExtentDescriptor extentDescriptor = new ExtentDescriptor();
                    @SuppressWarnings("unused")
                    int bytesRead = extentDescriptor.readFrom(extentData, a * 8);

                    if (blocksSeen + extentDescriptor.blockCount > block) {
                        extentLogicalStart[0] = blocksSeen * (long) context.getVolumeHeader().blockSize;
                        return extentDescriptor;
                    }

                    blocksSeen += extentDescriptor.blockCount;
                }
            } else {
                throw new IOException("Missing extent from extent overflow file: cnid=" + cnid + ", blocksSeen=" + blocksSeen);
            }
        }

        throw new UnsupportedOperationException("Requested file fragment beyond EOF");
    }
}
