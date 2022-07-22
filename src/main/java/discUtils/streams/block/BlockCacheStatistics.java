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

    private int freeReadBlocks;

    /**
     * Gets the number of free blocks in the read cache.
     */
    public int getFreeReadBlocks() {
        return freeReadBlocks;
    }

    public void setFreeReadBlocks(int value) {
        freeReadBlocks = value;
    }

    private long largeReadsIn;

    /**
     * Gets the number of requested 'large' reads, as defined by the
     * LargeReadSize setting.
     */
    public long getLargeReadsIn() {
        return largeReadsIn;
    }

    public void setLargeReadsIn(long value) {
        largeReadsIn = value;
    }

    private long readCacheHits;

    /**
     * Gets the number of times a read request was serviced (in part or whole)
     * from the cache.
     */
    public long getReadCacheHits() {
        return readCacheHits;
    }

    public void setReadCacheHits(long value) {
        readCacheHits = value;
    }

    private long readCacheMisses;

    /**
     * Gets the number of time a read request was serviced (in part or whole)
     * from the wrapped stream.
     */
    public long getReadCacheMisses() {
        return readCacheMisses;
    }

    public void setReadCacheMisses(long value) {
        readCacheMisses = value;
    }

    private long totalReadsIn;

    /**
     * Gets the total number of requested reads.
     */
    public long getTotalReadsIn() {
        return totalReadsIn;
    }

    public void setTotalReadsIn(long value) {
        totalReadsIn = value;
    }

    private long totalReadsOut;

    /**
     * Gets the total number of reads passed on by the cache.
     */
    public long getTotalReadsOut() {
        return totalReadsOut;
    }

    public void setTotalReadsOut(long value) {
        totalReadsOut = value;
    }

    private long totalWritesIn;

    /**
     * Gets the total number of requested writes.
     */
    public long getTotalWritesIn() {
        return totalWritesIn;
    }

    public void setTotalWritesIn(long value) {
        totalWritesIn = value;
    }

    private long unalignedReadsIn;

    /**
     * Gets the number of requested unaligned reads. Unaligned reads are reads
     * where the read doesn't start on a multiple of the block size.
     */
    public long getUnalignedReadsIn() {
        return unalignedReadsIn;
    }

    public void setUnalignedReadsIn(long value) {
        unalignedReadsIn = value;
    }

    private long unalignedWritesIn;

    /**
     * Gets the number of requested unaligned writes. Unaligned writes are
     * writes where the write doesn't start on a multiple of the block size.
     */
    public long getUnalignedWritesIn() {
        return unalignedWritesIn;
    }

    public void setUnalignedWritesIn(long value) {
        unalignedWritesIn = value;
    }
}
