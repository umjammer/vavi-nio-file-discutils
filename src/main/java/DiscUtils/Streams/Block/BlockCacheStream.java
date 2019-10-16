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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.SeekOrigin;


/**
 * A stream implementing a block-oriented read cache.
 */
public final class BlockCacheStream extends SparseStream {
    private boolean _atEof;

    private final int _blocksInReadBuffer;

    private final BlockCache<Block> _cache;

    private final Ownership _ownWrapped;

    private long _position;

    private final byte[] _readBuffer;

    private final BlockCacheSettings _settings;

    private final BlockCacheStatistics _stats;

    private SparseStream _wrappedStream;

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

        _wrappedStream = toWrap;
        _ownWrapped = ownership;
        _settings = new BlockCacheSettings(settings);
        if (_settings.getOptimumReadSize() % _settings.getBlockSize() != 0) {
            throw new IllegalArgumentException("Invalid settings, OptimumReadSize must be a multiple of BlockSize");
        }

        _readBuffer = new byte[_settings.getOptimumReadSize()];
        _blocksInReadBuffer = _settings.getOptimumReadSize() / _settings.getBlockSize();
        int totalBlocks = (int) (_settings.getReadCacheSize() / _settings.getBlockSize());
        _cache = new BlockCache<>(_settings.getBlockSize(), totalBlocks);
        _stats = new BlockCacheStatistics();
        _stats.setFreeReadBlocks(totalBlocks);
    }

    /**
     * Gets an indication as to whether the stream can be read.
     */
    public boolean canRead() {
        return true;
    }

    /**
     * Gets an indication as to whether the stream position can be changed.
     */
    public boolean canSeek() {
        return true;
    }

    /**
     * Gets an indication as to whether the stream can be written to.
     */
    public boolean canWrite() {
        return _wrappedStream.canWrite();
    }

    /**
     * Gets the parts of the stream that are stored. This may be an empty
     * enumeration if all bytes are zero.
     */
    public List<StreamExtent> getExtents() {
        checkDisposed();
        return _wrappedStream.getExtents();
    }

    /**
     * Gets the length of the stream.
     */
    public long getLength() {
        checkDisposed();
        return _wrappedStream.getLength();
    }

    /**
     * Gets and sets the current stream position.
     */
    public long getPosition() {
        checkDisposed();
        return _position;
    }

    public void setPosition(long value) {
        checkDisposed();
        _position = value;
    }

    /**
     * Gets the performance statistics for this instance.
     */
    public BlockCacheStatistics getStatistics() {
        _stats.setFreeReadBlocks(_cache.getFreeBlockCount());
        return _stats;
    }

    /**
     * Gets the parts of a stream that are stored, within a specified range.
     *
     * @param start The offset of the first byte of interest.
     * @param count The number of bytes of interest.
     * @return An enumeration of stream extents, indicating stored bytes.
     */
    public List<StreamExtent> getExtentsInRange(long start, long count) {
        checkDisposed();
        return _wrappedStream.getExtentsInRange(start, count);
    }

    /**
     * Reads data from the stream.
     *
     * @param buffer The buffer to fill.
     * @param offset The buffer offset to start from.
     * @param count  The number of bytes to read.
     * @return The number of bytes read.
     */
    public int read(byte[] buffer, int offset, int count) {
        checkDisposed();
        if (_position >= getLength()) {
            if (_atEof) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to read beyond end of stream");
            }

            _atEof = true;
            return 0;
        }

        _stats.setTotalReadsIn(_stats.getTotalReadsIn() + 1);
        if (count > _settings.getLargeReadSize()) {
            _stats.setLargeReadsIn(_stats.getLargeReadsIn() + 1);
            _stats.setTotalReadsOut(_stats.getTotalReadsOut() + 1);
            _wrappedStream.setPosition(_position);
            int numRead = _wrappedStream.read(buffer, offset, count);
            _position = _wrappedStream.getPosition();
            if (_position >= getLength()) {
                _atEof = true;
            }

            return numRead;
        }

        int totalBytesRead = 0;
        boolean servicedFromCache = false;
        boolean servicedOutsideCache = false;
        int blockSize = _settings.getBlockSize();
        long firstBlock = _position / blockSize;
        int offsetInNextBlock = (int) (_position % blockSize);
        long endBlock = MathUtilities.ceil(Math.min(_position + count, getLength()), blockSize);
        int numBlocks = (int) (endBlock - firstBlock);
        if (offsetInNextBlock != 0) {
            _stats.setUnalignedReadsIn(_stats.getUnalignedReadsIn() + 1);
        }

        int blocksRead = 0;
        while (blocksRead < numBlocks) {
            // Read from the cache as much as possible
            Block[] block = new Block[1];
            while (blocksRead < numBlocks && _cache.tryGetBlock(firstBlock + blocksRead, block)) {
                int bytesToRead = Math.min(count - totalBytesRead, block[0].getAvailable() - offsetInNextBlock);
                System.arraycopy(block[0].getData(), offsetInNextBlock, buffer, offset + totalBytesRead, bytesToRead);
                offsetInNextBlock = 0;
                totalBytesRead += bytesToRead;
                _position += bytesToRead;
                blocksRead++;
                servicedFromCache = true;
            }
            // Now handle a sequence of (one or more) blocks that are not cached
            if (blocksRead < numBlocks && !_cache.containsBlock(firstBlock + blocksRead)) {
                servicedOutsideCache = true;
                // Figure out how many blocks to read from the wrapped stream
                int blocksToRead = 0;
                while (blocksRead + blocksToRead < numBlocks && blocksToRead < _blocksInReadBuffer
                        && !_cache.containsBlock(firstBlock + blocksRead + blocksToRead)) {
                    ++blocksToRead;
                }
                // Allow for the end of the stream not being block-aligned
                long readPosition = (firstBlock + blocksRead) * blockSize;
                int bytesRead = (int) Math.min(blocksToRead * (long) blockSize, getLength() - readPosition);
                // Do the read
                _stats.setTotalReadsOut(_stats.getTotalReadsOut() + 1);
                _wrappedStream.setPosition(readPosition);
                StreamUtilities.readExact(_wrappedStream, _readBuffer, 0, bytesRead);
                for (int i = 0; i < blocksToRead; ++i) {
                    // Cache the read blocks
                    int copyBytes = Math.min(blockSize, bytesRead - i * blockSize);
                    block[0] = _cache.getBlock(firstBlock + blocksRead + i, Block.class);
                    System.arraycopy(_readBuffer, i * blockSize, block[0].getData(), 0, copyBytes);
                    block[0].setAvailable(copyBytes);
                    if (copyBytes < blockSize) {
                        Arrays.fill(_readBuffer,
                                    i * blockSize + copyBytes,
                                    (i * blockSize + copyBytes) + (blockSize - copyBytes),
                                    (byte) 0);
                    }

                }
                blocksRead += blocksToRead;
                // Propogate the data onto the caller
                int bytesToCopy = Math.min(count - totalBytesRead, bytesRead - offsetInNextBlock);
                System.arraycopy(_readBuffer, offsetInNextBlock, buffer, offset + totalBytesRead, bytesToCopy);
                totalBytesRead += bytesToCopy;
                _position += bytesToCopy;
                offsetInNextBlock = 0;
            }

        }
        if (_position >= getLength() && totalBytesRead == 0) {
            _atEof = true;
        }

        if (servicedFromCache) {
            _stats.setReadCacheHits(_stats.getReadCacheHits() + 1);
        }

        if (servicedOutsideCache) {
            _stats.setReadCacheMisses(_stats.getReadCacheMisses() + 1);
        }

        return totalBytesRead;
    }

    /**
     * Flushes the stream.
     */
    public void flush() {
        checkDisposed();
        _wrappedStream.flush();
    }

    /**
     * Moves the stream position.
     *
     * @param offset The origin-relative location.
     * @param origin The base location.
     * @return The new absolute stream position.
     */
    public long seek(long offset, SeekOrigin origin) {
        checkDisposed();
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += _position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += getLength();
        }

        _atEof = false;
        if (effectiveOffset < 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to move before beginning of disk");
        }

        _position = effectiveOffset;
        return _position;
    }

    /**
     * Sets the length of the stream.
     *
     * @param value The new length.
     */
    public void setLength(long value) {
        checkDisposed();
        _wrappedStream.setLength(value);
    }

    /**
     * Writes data to the stream at the current location.
     *
     * @param buffer The data to write.
     * @param offset The first byte to write from buffer.
     * @param count  The number of bytes to write.
     */
    public void write(byte[] buffer, int offset, int count) {
        checkDisposed();
        _stats.setTotalWritesIn(_stats.getTotalWritesIn() + 1);
        int blockSize = _settings.getBlockSize();
        long firstBlock = _position / blockSize;
        long endBlock = MathUtilities.ceil(Math.min(_position + count, getLength()), blockSize);
        int numBlocks = (int) (endBlock - firstBlock);
        try {
            _wrappedStream.setPosition(_position);
            _wrappedStream.write(buffer, offset, count);
        } catch (Exception __dummyCatchVar0) {
            invalidateBlocks(firstBlock, numBlocks);
            throw __dummyCatchVar0;
        }

        int offsetInNextBlock = (int) (_position % blockSize);
        if (offsetInNextBlock != 0) {
            _stats.setUnalignedWritesIn(_stats.getUnalignedWritesIn() + 1);
        }

        // For each block touched, if it's cached, update it
        int bytesProcessed = 0;
        for (int i = 0; i < numBlocks; ++i) {
            int bufferPos = offset + bytesProcessed;
            int bytesThisBlock = Math.min(count - bytesProcessed, blockSize - offsetInNextBlock);
            Block[] block = new Block[1];
            boolean boolVar___0 = _cache.tryGetBlock(firstBlock + i, block);
            if (boolVar___0) {
                System.arraycopy(buffer, bufferPos, block[0].getData(), offsetInNextBlock, bytesThisBlock);
                block[0].setAvailable(Math.max(block[0].getAvailable(), offsetInNextBlock + bytesThisBlock));
            }

            offsetInNextBlock = 0;
            bytesProcessed += bytesThisBlock;
        }
        _position += count;
    }

    /**
     * Disposes of this instance, freeing up associated resources.
     */
    public void close() throws IOException {
        if (_wrappedStream != null && _ownWrapped == Ownership.Dispose) {
            _wrappedStream.close();
        }

        _wrappedStream = null;
    }

    private void checkDisposed() {
        if (_wrappedStream == null) {
            throw new moe.yo3explorer.dotnetio4j.IOException("it has been closed.");
        }
    }

    private void invalidateBlocks(long firstBlock, int numBlocks) {
        for (long i = firstBlock; i < firstBlock + numBlocks; ++i) {
            _cache.releaseBlock(i);
        }
    }
}
