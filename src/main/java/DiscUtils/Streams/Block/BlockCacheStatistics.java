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
    private int __FreeReadBlocks;

    public int getFreeReadBlocks() {
        return __FreeReadBlocks;
    }

    public void setFreeReadBlocks(int value) {
        __FreeReadBlocks = value;
    }

    /**
     * Gets the number of requested 'large' reads, as defined by the
     * LargeReadSize setting.
     */
    private long __LargeReadsIn;

    public long getLargeReadsIn() {
        return __LargeReadsIn;
    }

    public void setLargeReadsIn(long value) {
        __LargeReadsIn = value;
    }

    /**
     * Gets the number of times a read request was serviced (in part or whole)
     * from the cache.
     */
    private long __ReadCacheHits;

    public long getReadCacheHits() {
        return __ReadCacheHits;
    }

    public void setReadCacheHits(long value) {
        __ReadCacheHits = value;
    }

    /**
     * Gets the number of time a read request was serviced (in part or whole)
     * from the wrapped stream.
     */
    private long __ReadCacheMisses;

    public long getReadCacheMisses() {
        return __ReadCacheMisses;
    }

    public void setReadCacheMisses(long value) {
        __ReadCacheMisses = value;
    }

    /**
     * Gets the total number of requested reads.
     */
    private long __TotalReadsIn;

    public long getTotalReadsIn() {
        return __TotalReadsIn;
    }

    public void setTotalReadsIn(long value) {
        __TotalReadsIn = value;
    }

    /**
     * Gets the total number of reads passed on by the cache.
     */
    private long __TotalReadsOut;

    public long getTotalReadsOut() {
        return __TotalReadsOut;
    }

    public void setTotalReadsOut(long value) {
        __TotalReadsOut = value;
    }

    /**
     * Gets the total number of requested writes.
     */
    private long __TotalWritesIn;

    public long getTotalWritesIn() {
        return __TotalWritesIn;
    }

    public void setTotalWritesIn(long value) {
        __TotalWritesIn = value;
    }

    /**
     * Gets the number of requested unaligned reads.
     * Unaligned reads are reads where the read doesn't start on a multiple of
     * the block size.
     */
    private long __UnalignedReadsIn;

    public long getUnalignedReadsIn() {
        return __UnalignedReadsIn;
    }

    public void setUnalignedReadsIn(long value) {
        __UnalignedReadsIn = value;
    }

    /**
     * Gets the number of requested unaligned writes.
     * Unaligned writes are writes where the write doesn't start on a multiple
     * of
     * the block size.
     */
    private long __UnalignedWritesIn;

    public long getUnalignedWritesIn() {
        return __UnalignedWritesIn;
    }

    public void setUnalignedWritesIn(long value) {
        __UnalignedWritesIn = value;
    }

}
