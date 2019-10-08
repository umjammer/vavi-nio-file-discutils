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

import java.io.Serializable;
import java.util.List;

import DiscUtils.Streams.StreamExtent;


/**
 * Abstract base class for implementations of IBuffer.
 */
public abstract class Buffer implements Serializable, IBuffer {
    /**
     * Gets a value indicating whether this buffer can be read.
     */
    public abstract boolean canRead();

    /**
     * Gets a value indicating whether this buffer can be modified.
     */
    public abstract boolean canWrite();

    /**
     * Gets the current capacity of the buffer, in bytes.
     */
    public abstract long getCapacity();

    /**
     * Gets the parts of the stream that are stored.
     * This may be an empty enumeration if all bytes are zero.
     */
    public List<StreamExtent> getExtents() {
        return getExtentsInRange(0, getCapacity());
    }

    /**
     * Reads from the buffer into a byte array.
     *
     * @param pos The offset within the buffer to start reading.
     * @param buffer The destination byte array.
     * @param offset The start offset within the destination buffer.
     * @param count The number of bytes to read.
     * @return The actual number of bytes read.
     */
    public abstract int read(long pos, byte[] buffer, int offset, int count);

    /**
     * Writes a byte array into the buffer.
     *
     * @param pos The start offset within the buffer.
     * @param buffer The source byte array.
     * @param offset The start offset within the source byte array.
     * @param count The number of bytes to write.
     */
    public abstract void write(long pos, byte[] buffer, int offset, int count);

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
    public void clear(long pos, int count) {
        write(pos, new byte[count], 0, count);
    }

    /**
     * Flushes all data to the underlying storage.
     * The default behaviour, implemented by this class, is to take no action.
     */
    public void flush() {
    }

    /**
     * Sets the capacity of the buffer, truncating if appropriate.
     *
     * @param value The desired capacity of the buffer.
     */
    public abstract void setCapacity(long value);

    /**
     * Gets the parts of a buffer that are stored, within a specified range.
     *
     * @param start The offset of the first byte of interest.
     * @param count The number of bytes of interest.
     * @return An enumeration of stream extents, indicating stored bytes.
     */
    public abstract List<StreamExtent> getExtentsInRange(long start, long count);

}
