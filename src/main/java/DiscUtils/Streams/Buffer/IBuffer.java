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

package DiscUtils.Streams.Buffer;

import java.util.List;

import DiscUtils.Streams.StreamExtent;


/**
 * Interface shared by all buffers.
 * 
 * Buffers are very similar to streams, except the buffer has no notion of
 * 'current position'. All I/O operations instead specify the position, as
 * needed. Buffers also support sparse behaviour.
 */
public interface IBuffer {
    /**
     * Gets a value indicating whether this buffer can be read.
     */
    boolean canRead();

    /**
     * Gets a value indicating whether this buffer can be modified.
     */
    boolean canWrite();

    /**
     * Gets the current capacity of the buffer, in bytes.
     */
    long getCapacity();

    /**
     * Gets the parts of the buffer that are stored.
     * This may be an empty enumeration if all bytes are zero.
     */
    List<StreamExtent> getExtents();

    /**
     * Reads from the buffer into a byte array.
     * 
     * @param pos The offset within the buffer to start reading.
     * @param buffer The destination byte array.
     * @param offset The start offset within the destination buffer.
     * @param count The number of bytes to read.
     * @return The actual number of bytes read.
     */
    int read(long pos, byte[] buffer, int offset, int count);

    /**
     * Writes a byte array into the buffer.
     * 
     * @param pos The start offset within the buffer.
     * @param buffer The source byte array.
     * @param offset The start offset within the source byte array.
     * @param count The number of bytes to write.
     */
    void write(long pos, byte[] buffer, int offset, int count);

    /**
     * Clears bytes from the buffer.
     * 
     * @param pos The start offset within the buffer.
     * @param count The number of bytes to clear.Logically equivalent to writing
     *            {@code count}
     *            null/zero bytes to the buffer, some
     *            implementations determine that some (or all) of the range
     *            indicated is not actually
     *            stored. There is no direct, automatic, correspondence to
     *            clearing bytes and them
     *            not being represented as an 'extent' - for example, the
     *            implementation of the underlying
     *            stream may not permit fine-grained extent storage.It is always
     *            safe to call this method to 'zero-out' a section of a buffer,
     *            regardless of
     *            the underlying buffer implementation.
     */
    void clear(long pos, int count);

    /**
     * Flushes all data to the underlying storage.
     */
    void flush();

    /**
     * Sets the capacity of the buffer, truncating if appropriate.
     * 
     * @param value The desired capacity of the buffer.
     */
    void setCapacity(long value);

    /**
     * Gets the parts of a buffer that are stored, within a specified range.
     * 
     * @param start The offset of the first byte of interest.
     * @param count The number of bytes of interest.
     * @return An enumeration of stream extents, indicating stored bytes.
     */
    List<StreamExtent> getExtentsInRange(long start, long count);

}
