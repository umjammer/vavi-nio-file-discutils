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

package DiscUtils.Streams.Block;

/**
 * Statistical information about the effectiveness of a BlockCache instance.
 */
public final class BlockCacheStatistics {
    /**
     * Gets the number of free blocks in the read cache.
     */
    private int _freeReadBlocks;

    public int getFreeReadBlocks() {
        return _freeReadBlocks;
    }

    public void setFreeReadBlocks(int value) {
        _freeReadBlocks = value;
    }

    /**
     * Gets the number of requested 'large' reads, as defined by the
     * LargeReadSize setting.
     */
    private long _largeReadsIn;

    public long getLargeReadsIn() {
        return _largeReadsIn;
    }

    public void setLargeReadsIn(long value) {
        _largeReadsIn = value;
    }

    /**
     * Gets the number of times a read request was serviced (in part or whole)
     * from the cache.
     */
    private long _readCacheHits;

    public long getReadCacheHits() {
        return _readCacheHits;
    }

    public void setReadCacheHits(long value) {
        _readCacheHits = value;
    }

    /**
     * Gets the number of time a read request was serviced (in part or whole)
     * from the wrapped stream.
     */
    private long _readCacheMisses;

    public long getReadCacheMisses() {
        return _readCacheMisses;
    }

    public void setReadCacheMisses(long value) {
        _readCacheMisses = value;
    }

    /**
     * Gets the total number of requested reads.
     */
    private long _totalReadsIn;

    public long getTotalReadsIn() {
        return _totalReadsIn;
    }

    public void setTotalReadsIn(long value) {
        _totalReadsIn = value;
    }

    /**
     * Gets the total number of reads passed on by the cache.
     */
    private long _totalReadsOut;

    public long getTotalReadsOut() {
        return _totalReadsOut;
    }

    public void setTotalReadsOut(long value) {
        _totalReadsOut = value;
    }

    /**
     * Gets the total number of requested writes.
     */
    private long _totalWritesIn;

    public long getTotalWritesIn() {
        return _totalWritesIn;
    }

    public void setTotalWritesIn(long value) {
        _totalWritesIn = value;
    }

    /**
     * Gets the number of requested unaligned reads. Unaligned reads are reads
     * where the read doesn't start on a multiple of the block size.
     */
    private long _unalignedReadsIn;

    public long getUnalignedReadsIn() {
        return _unalignedReadsIn;
    }

    public void setUnalignedReadsIn(long value) {
        _unalignedReadsIn = value;
    }

    /**
     * Gets the number of requested unaligned writes. Unaligned writes are
     * writes where the write doesn't start on a multiple of the block size.
     */
    private long _unalignedWritesIn;

    public long getUnalignedWritesIn() {
        return _unalignedWritesIn;
    }

    public void setUnalignedWritesIn(long value) {
        _unalignedWritesIn = value;
    }
}
