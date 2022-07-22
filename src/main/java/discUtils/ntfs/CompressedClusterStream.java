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

package discUtils.ntfs;

import java.util.Arrays;
import java.util.List;

import discUtils.core.compression.BlockCompressor;
import discUtils.core.compression.CompressionResult;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Range;
import dotnet4j.io.IOException;


public final class CompressedClusterStream extends ClusterStream {

    private final NtfsAttribute attr;

    private final int bytesPerCluster;

    private final byte[] cacheBuffer;

    private long cacheBufferVcn = -1;

    private final INtfsContext context;

    private final byte[] ioBuffer;

    private final RawClusterStream rawStream;

    public CompressedClusterStream(INtfsContext context, NtfsAttribute attr, RawClusterStream rawStream) {
        this.context = context;
        this.attr = attr;
        this.rawStream = rawStream;
        bytesPerCluster = this.context.getBiosParameterBlock().getBytesPerCluster();
        cacheBuffer = new byte[this.attr.getCompressionUnitSize() * context.getBiosParameterBlock().getBytesPerCluster()];
        ioBuffer = new byte[this.attr.getCompressionUnitSize() * context.getBiosParameterBlock().getBytesPerCluster()];
    }

    public long getAllocatedClusterCount() {
        return rawStream.getAllocatedClusterCount();
    }

    public List<Range> getStoredClusters() {
        return Range.chunked(rawStream.getStoredClusters(), attr.getCompressionUnitSize());
    }

    public boolean isClusterStored(long vcn) {
        return rawStream.isClusterStored(compressionStart(vcn));
    }

    public void expandToClusters(long numVirtualClusters, NonResidentAttributeRecord extent, boolean allocate) {
        rawStream.expandToClusters(MathUtilities.roundUp(numVirtualClusters, attr.getCompressionUnitSize()), extent, false);
    }

    public void truncateToClusters(long numVirtualClusters) {
        long alignedNum = MathUtilities.roundUp(numVirtualClusters, attr.getCompressionUnitSize());
        rawStream.truncateToClusters(alignedNum);
        if (alignedNum != numVirtualClusters) {
            rawStream.releaseClusters(numVirtualClusters, (int) (alignedNum - numVirtualClusters));
        }
    }

    public void readClusters(long startVcn, int count, byte[] buffer, int offset) {
        if (buffer.length < count * bytesPerCluster + offset) {
            throw new IllegalArgumentException("Cluster buffer too small");
        }

        int totalRead = 0;
        while (totalRead < count) {
            long focusVcn = startVcn + totalRead;
            loadCache(focusVcn);
            int cacheOffset = (int) (focusVcn - cacheBufferVcn);
            int toCopy = Math.min(attr.getCompressionUnitSize() - cacheOffset, count - totalRead);
            System.arraycopy(cacheBuffer,
                             cacheOffset * bytesPerCluster,
                             buffer,
                             offset + totalRead * bytesPerCluster,
                             toCopy * bytesPerCluster);
            totalRead += toCopy;
        }
    }

    public int writeClusters(long startVcn, int count, byte[] buffer, int offset) {
        if (buffer.length < count * bytesPerCluster + offset) {
            throw new IllegalArgumentException("Cluster buffer too small");
        }

        int totalAllocated = 0;
        int totalWritten = 0;
        while (totalWritten < count) {
            long focusVcn = startVcn + totalWritten;
            long cuStart = compressionStart(focusVcn);
            if (cuStart == focusVcn && count - totalWritten >= attr.getCompressionUnitSize()) {
                // Aligned write...
                totalAllocated += compressAndWriteClusters(focusVcn,
                                                           attr.getCompressionUnitSize(),
                                                           buffer,
                                                           offset + totalWritten * bytesPerCluster);
                totalWritten += attr.getCompressionUnitSize();
            } else {
                // Unaligned, so go through cache
                loadCache(focusVcn);
                int cacheOffset = (int) (focusVcn - cacheBufferVcn);
                int toCopy = Math.min(count - totalWritten, attr.getCompressionUnitSize() - cacheOffset);
                System.arraycopy(buffer,
                                 offset + totalWritten * bytesPerCluster,
                        cacheBuffer,
                                 cacheOffset * bytesPerCluster,
                                 toCopy * bytesPerCluster);
                totalAllocated += compressAndWriteClusters(cacheBufferVcn, attr.getCompressionUnitSize(), cacheBuffer, 0);
                totalWritten += toCopy;
            }
        }
        return totalAllocated;
    }

    public int clearClusters(long startVcn, int count) {
        int totalReleased = 0;
        int totalCleared = 0;
        while (totalCleared < count) {
            long focusVcn = startVcn + totalCleared;
            if (compressionStart(focusVcn) == focusVcn && count - totalCleared >= attr.getCompressionUnitSize()) {
                // Aligned - so it's a sparse compression unit...
                totalReleased += rawStream.releaseClusters(startVcn, attr.getCompressionUnitSize());
                totalCleared += attr.getCompressionUnitSize();
            } else {
                int toZero = (int) Math.min(count - totalCleared,
                                            attr.getCompressionUnitSize() - (focusVcn - compressionStart(focusVcn)));
                totalReleased -= writeZeroClusters(focusVcn, toZero);
                totalCleared += toZero;
            }
        }
        return totalReleased;
    }

    private int writeZeroClusters(long focusVcn, int count) {
        int allocatedClusters = 0;
        byte[] zeroBuffer = new byte[16 * bytesPerCluster];
        int numWritten = 0;
        while (numWritten < count) {
            int toWrite = Math.min(count - numWritten, 16);
            allocatedClusters += writeClusters(focusVcn + numWritten, toWrite, zeroBuffer, 0);
            numWritten += toWrite;
        }
        return allocatedClusters;
    }

    private int compressAndWriteClusters(long focusVcn, int count, byte[] buffer, int offset) {
        BlockCompressor compressor = context.getOptions().getCompressor();
        compressor.setBlockSize(bytesPerCluster);
        int totalAllocated = 0;
        int[] compressedLength = new int[] {
            ioBuffer.length
        };
        CompressionResult result = compressor
                .compress(buffer, offset, attr.getCompressionUnitSize() * bytesPerCluster, ioBuffer, 0, compressedLength);
        if (result == CompressionResult.AllZeros) {
            totalAllocated -= rawStream.releaseClusters(focusVcn, count);
        } else if (result == CompressionResult.Compressed &&
                   attr.getCompressionUnitSize() * bytesPerCluster - compressedLength[0] > bytesPerCluster) {
            int compClusters = MathUtilities.ceil(compressedLength[0], bytesPerCluster);
            totalAllocated += rawStream.allocateClusters(focusVcn, compClusters);
            totalAllocated += rawStream.writeClusters(focusVcn, compClusters, ioBuffer, 0);
            totalAllocated -= rawStream.releaseClusters(focusVcn + compClusters,
                                                         attr.getCompressionUnitSize() - compClusters);
        } else {
            totalAllocated += rawStream.allocateClusters(focusVcn, attr.getCompressionUnitSize());
            totalAllocated += rawStream.writeClusters(focusVcn, attr.getCompressionUnitSize(), buffer, offset);
        }
        return totalAllocated;
    }

    private long compressionStart(long vcn) {
        return MathUtilities.roundDown(vcn, attr.getCompressionUnitSize());
    }

    private void loadCache(long vcn) {
        long cuStart = compressionStart(vcn);
        if (cacheBufferVcn != cuStart) {
            if (rawStream.areAllClustersStored(cuStart, attr.getCompressionUnitSize())) {
                // Uncompressed data - read straight into cache buffer
                rawStream.readClusters(cuStart, attr.getCompressionUnitSize(), cacheBuffer, 0);
            } else if (rawStream.isClusterStored(cuStart)) {
                // Compressed data - read via IO buffer
                rawStream.readClusters(cuStart, attr.getCompressionUnitSize(), ioBuffer, 0);
                int expected = (int) Math.min(attr.getLength() - vcn * bytesPerCluster,
                        (long) attr.getCompressionUnitSize() * bytesPerCluster);
                int decomp = context.getOptions().getCompressor().decompress(ioBuffer, 0, ioBuffer.length, cacheBuffer, 0);
                if (decomp < expected) {
                    throw new IOException("Decompression returned too little data");
                }
            } else {
                // Sparse, wipe cache buffer directly
                Arrays.fill(cacheBuffer, 0, 0 + cacheBuffer.length, (byte) 0);
            }
            cacheBufferVcn = cuStart;
        }
    }
}
