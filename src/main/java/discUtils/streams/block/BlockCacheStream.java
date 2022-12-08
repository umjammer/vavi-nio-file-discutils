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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.SeekOrigin;


/**
 * A stream implementing a block-oriented read cache.
 */
public final class BlockCacheStream extends SparseStream {

    private boolean atEof;

    private final int blocksInReadBuffer;

    private final BlockCache<Block> cache;

    private final Ownership ownWrapped;

    private long position;

    private final byte[] readBuffer;

    private final BlockCacheSettings settings;

    private final BlockCacheStatistics stats;

    private SparseStream wrappedStream;

    /**
     * Initializes a new instance of the BlockCacheStream class.
     *
     * @param toWrap    The stream to wrap.
     * @param ownership Whether to assume ownership of {@code toWrap} .
     */
    public BlockCacheStream(SparseStream toWrap, Ownership ownership) {
        this(toWrap, ownership, new BlockCacheSettings());
    }

    /**
     * Initializes a new instance of the BlockCacheStream class.
     *
     * @param toWrap    The stream to wrap.
     * @param ownership Whether to assume ownership of {@code toWrap} .
     * @param settings  The cache settings.
     */
    public BlockCacheStream(SparseStream toWrap, Ownership ownership, BlockCacheSettings settings) {
        if (!toWrap.canRead()) {
            throw new IllegalArgumentException("The wrapped stream does not support reading");
        }

        if (!toWrap.canSeek()) {
            throw new IllegalArgumentException("The wrapped stream does not support seeking");
        }

        wrappedStream = toWrap;
        ownWrapped = ownership;
        this.settings = new BlockCacheSettings(settings);

        if (this.settings.getOptimumReadSize() % this.settings.getBlockSize() != 0) {
            throw new IllegalArgumentException("Invalid settings, OptimumReadSize must be a multiple of BlockSize");
        }

        readBuffer = new byte[this.settings.getOptimumReadSize()];
        blocksInReadBuffer = this.settings.getOptimumReadSize() / this.settings.getBlockSize();

        int totalBlocks = (int) (this.settings.getReadCacheSize() / this.settings.getBlockSize());

        cache = new BlockCache<>(this.settings.getBlockSize(), totalBlocks);
        stats = new BlockCacheStatistics();
        stats.setFreeReadBlocks(totalBlocks);
    }

    /**
     * Gets an indication as to whether the stream can be read.
     */
    @Override public boolean canRead() {
        return true;
    }

    /**
     * Gets an indication as to whether the stream position can be changed.
     */
    @Override public boolean canSeek() {
        return true;
    }

    /**
     * Gets an indication as to whether the stream can be written to.
     */
    @Override public boolean canWrite() {
        return wrappedStream.canWrite();
    }

    /**
     * Gets the parts of the stream that are stored. This may be an empty
     * enumeration if all bytes are zero.
     */
    @Override public List<StreamExtent> getExtents() {
        checkDisposed();
        return wrappedStream.getExtents();
    }

    /**
     * Gets the length of the stream.
     */
    @Override public long getLength() {
        checkDisposed();
        return wrappedStream.getLength();
    }

    /**
     * Gets and sets the current stream position.
     */
    @Override public long position() {
        checkDisposed();
        return position;
    }

    @Override public void position(long value) {
        checkDisposed();
        position = value;
    }

    /**
     * Gets the performance statistics for this instance.
     */
    public BlockCacheStatistics getStatistics() {
        stats.setFreeReadBlocks(cache.getFreeBlockCount());
        return stats;
    }

    /**
     * Gets the parts of a stream that are stored, within a specified range.
     *
     * @param start The offset of the first byte of interest.
     * @param count The number of bytes of interest.
     * @return An enumeration of stream extents, indicating stored bytes.
     */
    @Override public List<StreamExtent> getExtentsInRange(long start, long count) {
        checkDisposed();
        return wrappedStream.getExtentsInRange(start, count);
    }

    /**
     * Reads data from the stream.
     *
     * @param buffer The buffer to fill.
     * @param offset The buffer offset to start from.
     * @param count  The number of bytes to read.
     * @return The number of bytes read.
     */
    @Override public synchronized int read(byte[] buffer, int offset, int count) {
        checkDisposed();

        if (position >= getLength()) {
            if (atEof) {
                throw new dotnet4j.io.IOException("Attempt to read beyond end of stream");
            }
            atEof = true;
            return 0;
        }

        stats.setTotalReadsIn(stats.getTotalReadsIn() + 1);

        if (count > settings.getLargeReadSize()) {
            stats.setLargeReadsIn(stats.getLargeReadsIn() + 1);
            stats.setTotalReadsOut(stats.getTotalReadsOut() + 1);
            wrappedStream.position(position);
            int numRead = wrappedStream.read(buffer, offset, count);
            position = wrappedStream.position();

            if (position >= getLength()) {
                atEof = true;
            }

            return numRead;
        }

        int totalBytesRead = 0;
        boolean servicedFromCache = false;
        boolean servicedOutsideCache = false;
        int blockSize = settings.getBlockSize();

        long firstBlock = position / blockSize;
        int offsetInNextBlock = (int) (position % blockSize);
        long endBlock = MathUtilities.ceil(Math.min(position + count, getLength()), blockSize);
        int numBlocks = (int) (endBlock - firstBlock);

        if (offsetInNextBlock != 0) {
            stats.setUnalignedReadsIn(stats.getUnalignedReadsIn() + 1);
        }

        int blocksRead = 0;
        while (blocksRead < numBlocks) {
            Block[] block = new Block[1];

            // Read from the cache as much as possible
            while (blocksRead < numBlocks && cache.tryGetBlock(firstBlock + blocksRead, block)) {
                int bytesToRead = Math.min(count - totalBytesRead, block[0].getAvailable() - offsetInNextBlock);

                System.arraycopy(block[0].getData(), offsetInNextBlock, buffer, offset + totalBytesRead, bytesToRead);

                offsetInNextBlock = 0;
                totalBytesRead += bytesToRead;
                position += bytesToRead;
                blocksRead++;

                servicedFromCache = true;
            }

            // Now handle a sequence of (one or more) blocks that are not cached
            if (blocksRead < numBlocks && !cache.containsBlock(firstBlock + blocksRead)) {
                servicedOutsideCache = true;

                // Figure out how many blocks to read from the wrapped stream
                int blocksToRead = 0;
                while (blocksRead + blocksToRead < numBlocks && blocksToRead < blocksInReadBuffer
                        && !cache.containsBlock(firstBlock + blocksRead + blocksToRead)) {
                    ++blocksToRead;
                }

                // Allow for the end of the stream not being block-aligned
                long readPosition = (firstBlock + blocksRead) * blockSize;
                int bytesRead = (int) Math.min(blocksToRead * (long) blockSize, getLength() - readPosition);

                // Do the read
                stats.setTotalReadsOut(stats.getTotalReadsOut() + 1);
                wrappedStream.position(readPosition);
                StreamUtilities.readExact(wrappedStream, readBuffer, 0, bytesRead);

                // Cache the read blocks
                for (int i = 0; i < blocksToRead; ++i) {
                    int copyBytes = Math.min(blockSize, bytesRead - i * blockSize);
                    block[0] = cache.getBlock(firstBlock + blocksRead + i, Block.class);
                    System.arraycopy(readBuffer, i * blockSize, block[0].getData(), 0, copyBytes);
                    block[0].setAvailable(copyBytes);
                    if (copyBytes < blockSize) {
                        Arrays.fill(readBuffer,
                                    i * blockSize + copyBytes,
                                    (i * blockSize + copyBytes) + (blockSize - copyBytes),
                                    (byte) 0);
                    }

                }

                blocksRead += blocksToRead;

                // Propogate the data onto the caller
                int bytesToCopy = Math.min(count - totalBytesRead, bytesRead - offsetInNextBlock);
                System.arraycopy(readBuffer, offsetInNextBlock, buffer, offset + totalBytesRead, bytesToCopy);
                totalBytesRead += bytesToCopy;
                position += bytesToCopy;
                offsetInNextBlock = 0;
            }
        }

        if (position >= getLength() && totalBytesRead == 0) {
            atEof = true;
        }

        if (servicedFromCache) {
            stats.setReadCacheHits(stats.getReadCacheHits() + 1);
        }

        if (servicedOutsideCache) {
            stats.setReadCacheMisses(stats.getReadCacheMisses() + 1);
        }

        return totalBytesRead;
    }

    /**
     * Flushes the stream.
     */
    @Override public void flush() {
        checkDisposed();
        wrappedStream.flush();
    }

    /**
     * Moves the stream position.
     *
     * @param offset The origin-relative location.
     * @param origin The base location.
     * @return The new absolute stream position.
     */
    @Override public synchronized long seek(long offset, SeekOrigin origin) {
        checkDisposed();
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += getLength();
        }

        atEof = false;
        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
        }

        position = effectiveOffset;
        return position;
    }

    /**
     * Sets the length of the stream.
     *
     * @param value The new length.
     */
    @Override public void setLength(long value) {
        checkDisposed();
        wrappedStream.setLength(value);
    }

    /**
     * Writes data to the stream at the current location.
     *
     * @param buffer The data to write.
     * @param offset The first byte to write from buffer.
     * @param count  The number of bytes to write.
     */
    @Override public synchronized void write(byte[] buffer, int offset, int count) {
        checkDisposed();

        stats.setTotalWritesIn(stats.getTotalWritesIn() + 1);

        int blockSize = settings.getBlockSize();
        long firstBlock = position / blockSize;
        long endBlock = MathUtilities.ceil(Math.min(position + count, getLength()), blockSize);
        int numBlocks = (int) (endBlock - firstBlock);

        try {
            wrappedStream.position(position);
            wrappedStream.write(buffer, offset, count);
        } catch (Exception e) {
            invalidateBlocks(firstBlock, numBlocks);
            throw e;
        }

        int offsetInNextBlock = (int) (position % blockSize);
        if (offsetInNextBlock != 0) {
            stats.setUnalignedWritesIn(stats.getUnalignedWritesIn() + 1);
        }

        // For each block touched, if it's cached, update it
        int bytesProcessed = 0;
        for (int i = 0; i < numBlocks; ++i) {
            int bufferPos = offset + bytesProcessed;
            int bytesThisBlock = Math.min(count - bytesProcessed, blockSize - offsetInNextBlock);

            Block[] block = new Block[1];
            if (cache.tryGetBlock(firstBlock + i, block)) {
                System.arraycopy(buffer, bufferPos, block[0].getData(), offsetInNextBlock, bytesThisBlock);
                block[0].setAvailable(Math.max(block[0].getAvailable(), offsetInNextBlock + bytesThisBlock));
            }

            offsetInNextBlock = 0;
            bytesProcessed += bytesThisBlock;
        }
        position += count;
    }

    /**
     * Disposes of this instance, freeing up associated resources.
     */
    @Override public void close() throws IOException {
        if (wrappedStream != null && ownWrapped == Ownership.Dispose) {
            wrappedStream.close();
        }

        wrappedStream = null;
    }

    private void checkDisposed() {
        if (wrappedStream == null) {
            throw new dotnet4j.io.IOException("it has been closed.");
        }
    }

    private void invalidateBlocks(long firstBlock, int numBlocks) {
        for (long i = firstBlock; i < firstBlock + numBlocks; ++i) {
            cache.releaseBlock(i);
        }
    }
}
