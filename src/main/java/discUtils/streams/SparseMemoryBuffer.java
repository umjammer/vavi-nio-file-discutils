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

package discUtils.streams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import discUtils.streams.buffer.Buffer;


/**
 * A sparse in-memory buffer. This class is useful for storing large sparse
 * buffers in memory, unused chunks of the buffer are not stored (assumed to be
 * zero).
 */
public final class SparseMemoryBuffer extends Buffer {

    private final Map<Integer, byte[]> buffers;

    private long capacity;

    /**
     * Initializes a new instance of the SparseMemoryBuffer class.
     *
     * @param chunkSize The size of each allocation chunk.
     */
    public SparseMemoryBuffer(int chunkSize) {
        this.chunkSize = chunkSize;
        buffers = new HashMap<>();
    }

    /**
     * Gets the (sorted) list of allocated chunks, as chunk indexes.
     *
     * This method returns chunks as an index rather than absolute stream
     * position. For example, if ChunkSize is 16KB, and the first 32KB of the
     * buffer is actually stored, this method will return 0 and 1. This
     * indicates the first and second chunks are stored.
     *
     * @return An enumeration of chunk indexes.
     */
    public List<Integer> getAllocatedChunks() {
        List<Integer> keys = new ArrayList<>(buffers.keySet());
        Collections.sort(keys);
        return keys;
    }

    /**
     * Indicates this stream can be read (always {@code true} ).
     */
    @Override
    public boolean canRead() {
        return true;
    }

    /**
     * Indicates this stream can be written (always {@code true} ).
     */
    @Override
    public boolean canWrite() {
        return true;
    }

    /**
     * Gets the current capacity of the sparse buffer (number of logical bytes
     * stored).
     */
    @Override
    public long getCapacity() {
        return capacity;
    }

    /**
     * Gets the size of each allocation chunk.
     */
    private final int chunkSize;

    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * Accesses this memory buffer as an infinite byte array.
     *
     * @param pos The buffer position to read.
     * @return The byte stored at this position (or Zero if not explicitly
     *         stored).
     */
    public byte get(long pos) {
        byte[] buffer = new byte[1];
        if (read(pos, buffer, 0, 1) != 0) {
            return buffer[0];
        }

        return 0;
    }

    public void put(long pos, byte value) {
        byte[] buffer = new byte[1];
        buffer[0] = value;
        write(pos, buffer, 0, 1);
    }

    /**
     * Reads a section of the sparse buffer into a byte array.
     *
     * @param pos The offset within the sparse buffer to start reading.
     * @param buffer The destination byte array.
     * @param offset The start offset within the destination buffer.
     * @param count The number of bytes to read.
     * @return The actual number of bytes read.
     */
    @Override
    public int read(long pos, byte[] buffer, int offset, int count) {
        int totalRead = 0;
        while (count > 0 && pos < capacity) {
            int chunk = (int) (pos / getChunkSize());
            int chunkOffset = (int) (pos % getChunkSize());
            int numToRead = (int) Math.min(Math.min(getChunkSize() - chunkOffset, capacity - pos), count);
            if (!buffers.containsKey(chunk)) {
                Arrays.fill(buffer, offset, offset + numToRead, (byte) 0);
            } else {
                byte[] chunkBuffer = buffers.get(chunk);
                System.arraycopy(chunkBuffer, chunkOffset, buffer, offset, numToRead);
            }
            totalRead += numToRead;
            offset += numToRead;
            count -= numToRead;
            pos += numToRead;
        }
        return totalRead;
    }

    /**
     * Writes a byte array into the sparse buffer.
     *
     * @param pos The start offset within the sparse buffer.
     * @param buffer The source byte array.
     * @param offset The start offset within the source byte array.
     * @param count The number of bytes to write.
     */
    @Override
    public void write(long pos, byte[] buffer, int offset, int count) {
        while (count > 0) {
            int chunk = (int) (pos / getChunkSize());
            int chunkOffset = (int) (pos % getChunkSize());
            int numToWrite = Math.min(getChunkSize() - chunkOffset, count);
            byte[] chunkBuffer;
            if (!buffers.containsKey(chunk)) {
                chunkBuffer = new byte[getChunkSize()];
                buffers.put(chunk, chunkBuffer);
            }
            chunkBuffer = buffers.get(chunk);

            System.arraycopy(buffer, offset, chunkBuffer, chunkOffset, numToWrite);
            offset += numToWrite;
            count -= numToWrite;
            pos += numToWrite;
        }
        capacity = Math.max(capacity, pos);
    }

    /**
     * Clears bytes from the buffer.
     *
     * @param pos The start offset within the buffer.
     * @param count The number of bytes to clear.
     */
    @Override
    public void clear(long pos, int count) {
        while (count > 0) {
            int chunk = (int) (pos / getChunkSize());
            int chunkOffset = (int) (pos % getChunkSize());
            int numToClear = Math.min(getChunkSize() - chunkOffset, count);
            if (buffers.containsKey(chunk)) {
                if (chunkOffset == 0 && numToClear == getChunkSize()) {
                    buffers.remove(chunk);
                } else {
                    byte[] chunkBuffer = buffers.get(chunk);
                    Arrays.fill(chunkBuffer, chunkOffset, chunkOffset + numToClear, (byte) 0);
                }
            }

            count -= numToClear;
            pos += numToClear;
        }
        capacity = Math.max(capacity, pos);
    }

    /**
     * Sets the capacity of the sparse buffer, truncating if appropriate.
     *
     * This method does not allocate any chunks, it merely records the logical
     * capacity of the sparse buffer. Writes beyond the specified capacity will
     * increase the capacity.
     * 
     * @param value The desired capacity of the buffer.
     */
    @Override
    public void setCapacity(long value) {
        capacity = value;
    }

    /**
     * Gets the parts of a buffer that are stored, within a specified range.
     *
     * @param start The offset of the first byte of interest.
     * @param count The number of bytes of interest.
     * @return An enumeration of stream extents, indicating stored bytes.
     */
    @Override
    public List<StreamExtent> getExtentsInRange(long start, long count) {
        long end = start + count;
        return getAllocatedChunks().stream().filter(chunk -> {
            long chunkStart = chunk * (long) getChunkSize();
            long chunkEnd = chunkStart + getChunkSize();
            return chunkEnd > start && chunkStart < end;
        }).map(chunk -> {
            long chunkStart = chunk * (long) getChunkSize();
            long chunkEnd = chunkStart + getChunkSize();
            long extentStart = Math.max(start, chunkStart);
            return new StreamExtent(extentStart, Math.min(chunkEnd, end) - extentStart);
        }).collect(Collectors.toList());
    }
}
