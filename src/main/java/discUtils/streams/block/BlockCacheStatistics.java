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

package discUtils.streams.block;

/**
 * Statistical information about the effectiveness of a BlockCache instance.
 */
public final class BlockCacheStatistics {
    private int _freeReadBlocks;

    /**
     * Gets the number of free blocks in the read cache.
     */
    public int getFreeReadBlocks() {
        return _freeReadBlocks;
    }

    public void setFreeReadBlocks(int value) {
        _freeReadBlocks = value;
    }

    private long _largeReadsIn;

    /**
     * Gets the number of requested 'large' reads, as defined by the
     * LargeReadSize setting.
     */
    public long getLargeReadsIn() {
        return _largeReadsIn;
    }

    public void setLargeReadsIn(long value) {
        _largeReadsIn = value;
    }

    private long _readCacheHits;

    /**
     * Gets the number of times a read request was serviced (in part or whole)
     * from the cache.
     */
    public long getReadCacheHits() {
        return _readCacheHits;
    }

    public void setReadCacheHits(long value) {
        _readCacheHits = value;
    }

    private long _readCacheMisses;

    /**
     * Gets the number of time a read request was serviced (in part or whole)
     * from the wrapped stream.
     */
    public long getReadCacheMisses() {
        return _readCacheMisses;
    }

    public void setReadCacheMisses(long value) {
        _readCacheMisses = value;
    }

    private long _totalReadsIn;

    /**
     * Gets the total number of requested reads.
     */
    public long getTotalReadsIn() {
        return _totalReadsIn;
    }

    public void setTotalReadsIn(long value) {
        _totalReadsIn = value;
    }

    private long _totalReadsOut;

    /**
     * Gets the total number of reads passed on by the cache.
     */
    public long getTotalReadsOut() {
        return _totalReadsOut;
    }

    public void setTotalReadsOut(long value) {
        _totalReadsOut = value;
    }

    private long _totalWritesIn;

    /**
     * Gets the total number of requested writes.
     */
    public long getTotalWritesIn() {
        return _totalWritesIn;
    }

    public void setTotalWritesIn(long value) {
        _totalWritesIn = value;
    }

    private long _unalignedReadsIn;

    /**
     * Gets the number of requested unaligned reads. Unaligned reads are reads
     * where the read doesn't start on a multiple of the block size.
     */
    public long getUnalignedReadsIn() {
        return _unalignedReadsIn;
    }

    public void setUnalignedReadsIn(long value) {
        _unalignedReadsIn = value;
    }

    private long _unalignedWritesIn;

    /**
     * Gets the number of requested unaligned writes. Unaligned writes are
     * writes where the write doesn't start on a multiple of the block size.
     */
    public long getUnalignedWritesIn() {
        return _unalignedWritesIn;
    }

    public void setUnalignedWritesIn(long value) {
        _unalignedWritesIn = value;
    }
}
