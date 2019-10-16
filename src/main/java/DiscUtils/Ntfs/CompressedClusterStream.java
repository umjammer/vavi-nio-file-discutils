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

package DiscUtils.Ntfs;

import java.util.Arrays;
import java.util.List;

import DiscUtils.Core.Compression.BlockCompressor;
import DiscUtils.Core.Compression.CompressionResult;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Range;
import moe.yo3explorer.dotnetio4j.IOException;


public final class CompressedClusterStream extends ClusterStream {
    private final NtfsAttribute _attr;

    private final int _bytesPerCluster;

    private final byte[] _cacheBuffer;

    private long _cacheBufferVcn = -1;

    private final INtfsContext _context;

    private final byte[] _ioBuffer;

    private final RawClusterStream _rawStream;

    public CompressedClusterStream(INtfsContext context, NtfsAttribute attr, RawClusterStream rawStream) {
        _context = context;
        _attr = attr;
        _rawStream = rawStream;
        _bytesPerCluster = _context.getBiosParameterBlock().getBytesPerCluster();
        _cacheBuffer = new byte[_attr.getCompressionUnitSize() * context.getBiosParameterBlock().getBytesPerCluster()];
        _ioBuffer = new byte[_attr.getCompressionUnitSize() * context.getBiosParameterBlock().getBytesPerCluster()];
    }

    public long getAllocatedClusterCount() {
        return _rawStream.getAllocatedClusterCount();
    }

    public List<Range> getStoredClusters() {
        return Range.chunked(_rawStream.getStoredClusters(), _attr.getCompressionUnitSize());
    }

    public boolean isClusterStored(long vcn) {
        return _rawStream.isClusterStored(compressionStart(vcn));
    }

    public void expandToClusters(long numVirtualClusters, NonResidentAttributeRecord extent, boolean allocate) {
        _rawStream.expandToClusters(MathUtilities.roundUp(numVirtualClusters, _attr.getCompressionUnitSize()), extent, false);
    }

    public void truncateToClusters(long numVirtualClusters) {
        long alignedNum = MathUtilities.roundUp(numVirtualClusters, _attr.getCompressionUnitSize());
        _rawStream.truncateToClusters(alignedNum);
        if (alignedNum != numVirtualClusters) {
            _rawStream.releaseClusters(numVirtualClusters, (int) (alignedNum - numVirtualClusters));
        }

    }

    public void readClusters(long startVcn, int count, byte[] buffer, int offset) {
        if (buffer.length < count * _bytesPerCluster + offset) {
            throw new IllegalArgumentException("Cluster buffer too small");
        }

        int totalRead = 0;
        while (totalRead < count) {
            long focusVcn = startVcn + totalRead;
            loadCache(focusVcn);
            int cacheOffset = (int) (focusVcn - _cacheBufferVcn);
            int toCopy = Math.min(_attr.getCompressionUnitSize() - cacheOffset, count - totalRead);
            System.arraycopy(_cacheBuffer,
                             cacheOffset * _bytesPerCluster,
                             buffer,
                             offset + totalRead * _bytesPerCluster,
                             toCopy * _bytesPerCluster);
            totalRead += toCopy;
        }
    }

    public int writeClusters(long startVcn, int count, byte[] buffer, int offset) {
        if (buffer.length < count * _bytesPerCluster + offset) {
            throw new IllegalArgumentException("Cluster buffer too small");
        }

        int totalAllocated = 0;
        int totalWritten = 0;
        while (totalWritten < count) {
            long focusVcn = startVcn + totalWritten;
            long cuStart = compressionStart(focusVcn);
            if (cuStart == focusVcn && count - totalWritten >= _attr.getCompressionUnitSize()) {
                // Aligned write...
                totalAllocated += compressAndWriteClusters(focusVcn,
                                                           _attr.getCompressionUnitSize(),
                                                           buffer,
                                                           offset + totalWritten * _bytesPerCluster);
                totalWritten += _attr.getCompressionUnitSize();
            } else {
                // Unaligned, so go through cache
                loadCache(focusVcn);
                int cacheOffset = (int) (focusVcn - _cacheBufferVcn);
                int toCopy = Math.min(count - totalWritten, _attr.getCompressionUnitSize() - cacheOffset);
                System.arraycopy(buffer,
                                 offset + totalWritten * _bytesPerCluster,
                                 _cacheBuffer,
                                 cacheOffset * _bytesPerCluster,
                                 toCopy * _bytesPerCluster);
                totalAllocated += compressAndWriteClusters(_cacheBufferVcn, _attr.getCompressionUnitSize(), _cacheBuffer, 0);
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
            if (compressionStart(focusVcn) == focusVcn && count - totalCleared >= _attr.getCompressionUnitSize()) {
                // Aligned - so it's a sparse compression unit...
                totalReleased += _rawStream.releaseClusters(startVcn, _attr.getCompressionUnitSize());
                totalCleared += _attr.getCompressionUnitSize();
            } else {
                int toZero = (int) Math.min(count - totalCleared,
                                            _attr.getCompressionUnitSize() - (focusVcn - compressionStart(focusVcn)));
                totalReleased -= writeZeroClusters(focusVcn, toZero);
                totalCleared += toZero;
            }
        }
        return totalReleased;
    }

    private int writeZeroClusters(long focusVcn, int count) {
        int allocatedClusters = 0;
        byte[] zeroBuffer = new byte[16 * _bytesPerCluster];
        int numWritten = 0;
        while (numWritten < count) {
            int toWrite = Math.min(count - numWritten, 16);
            allocatedClusters += writeClusters(focusVcn + numWritten, toWrite, zeroBuffer, 0);
            numWritten += toWrite;
        }
        return allocatedClusters;
    }

    private int compressAndWriteClusters(long focusVcn, int count, byte[] buffer, int offset) {
        BlockCompressor compressor = _context.getOptions().getCompressor();
        compressor.setBlockSize(_bytesPerCluster);
        int totalAllocated = 0;
        int[] compressedLength = new int[] {
            _ioBuffer.length
        };
        CompressionResult result = compressor
                .compress(buffer, offset, _attr.getCompressionUnitSize() * _bytesPerCluster, _ioBuffer, 0, compressedLength);
        if (result == CompressionResult.AllZeros) {
            totalAllocated -= _rawStream.releaseClusters(focusVcn, count);
        } else if (result == CompressionResult.Compressed &&
                   _attr.getCompressionUnitSize() * _bytesPerCluster - compressedLength[0] > _bytesPerCluster) {
            int compClusters = MathUtilities.ceil(compressedLength[0], _bytesPerCluster);
            totalAllocated += _rawStream.allocateClusters(focusVcn, compClusters);
            totalAllocated += _rawStream.writeClusters(focusVcn, compClusters, _ioBuffer, 0);
            totalAllocated -= _rawStream.releaseClusters(focusVcn + compClusters,
                                                         _attr.getCompressionUnitSize() - compClusters);
        } else {
            totalAllocated += _rawStream.allocateClusters(focusVcn, _attr.getCompressionUnitSize());
            totalAllocated += _rawStream.writeClusters(focusVcn, _attr.getCompressionUnitSize(), buffer, offset);
        }
        return totalAllocated;
    }

    private long compressionStart(long vcn) {
        return MathUtilities.roundDown(vcn, _attr.getCompressionUnitSize());
    }

    private void loadCache(long vcn) {
        long cuStart = compressionStart(vcn);
        if (_cacheBufferVcn != cuStart) {
            if (_rawStream.areAllClustersStored(cuStart, _attr.getCompressionUnitSize())) {
                // Uncompressed data - read straight into cache buffer
                _rawStream.readClusters(cuStart, _attr.getCompressionUnitSize(), _cacheBuffer, 0);
            } else if (_rawStream.isClusterStored(cuStart)) {
                // Compressed data - read via IO buffer
                _rawStream.readClusters(cuStart, _attr.getCompressionUnitSize(), _ioBuffer, 0);
                int expected = (int) Math.min(_attr.getLength() - vcn * _bytesPerCluster,
                                              _attr.getCompressionUnitSize() * _bytesPerCluster);
                int decomp = _context.getOptions().getCompressor().decompress(_ioBuffer, 0, _ioBuffer.length, _cacheBuffer, 0);
                if (decomp < expected) {
                    throw new IOException("Decompression returned too little data");
                }
            } else {
                // Sparse, wipe cache buffer directly
                Arrays.fill(_cacheBuffer, 0, 0 + _cacheBuffer.length, (byte) 0);
            }
            _cacheBufferVcn = cuStart;
        }
    }
}
