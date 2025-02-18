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

package discUtils.streams.buffer;

import java.util.List;
import java.util.stream.Collectors;

import discUtils.streams.StreamExtent;


/**
 * Class representing a portion of an existing buffer.
 */
public class SubBuffer extends Buffer {

    private final long first;

    private final long length;

    private final IBuffer parent;

    /**
     * Initializes a new instance of the SubBuffer class.
     *
     * @param parent The parent buffer.
     * @param first The first byte in
     *            {@code parent}
     *            represented by this sub-buffer.
     * @param length The number of bytes of
     *            {@code parent}
     *            represented by this sub-buffer.
     */
    public SubBuffer(IBuffer parent, long first, long length) {
        this.parent = parent;
        this.first = first;
        this.length = length;
        if (this.first + this.length > this.parent.getCapacity()) {
            throw new IllegalArgumentException("Substream extends beyond end of parent stream");
        }

    }

    /**
     * Can this buffer be read.
     */
    @Override
    public boolean canRead() {
        return parent.canRead();
    }

    /**
     * Can this buffer be modified.
     */
    @Override
    public boolean canWrite() {
        return parent.canWrite();
    }

    /**
     * Gets the current capacity of the buffer, in bytes.
     */
    @Override
    public long getCapacity() {
        return length;
    }

    /**
     * Gets the parts of the buffer that are stored.
     * This may be an empty enumeration if all bytes are zero.
     */
    @Override
    public List<StreamExtent> getExtents() {
        return offsetExtents(parent.getExtentsInRange(first, length));
    }

    /**
     * Flushes all data to the underlying storage.
     */
    @Override
    public void flush() {
        parent.flush();
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
    @Override
    public int read(long pos, byte[] buffer, int offset, int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("Attempt to read negative bytes");
        }

        if (pos >= length) {
            return 0;
        }

        return parent.read(pos + first, buffer, offset, (int) Math.min(count, Math.min(length - pos, Integer.MAX_VALUE)));
    }

    /**
     * Writes a byte array into the buffer.
     *
     * @param pos The start offset within the buffer.
     * @param buffer The source byte array.
     * @param offset The start offset within the source byte array.
     * @param count The number of bytes to write.
     */
    @Override
    public void write(long pos, byte[] buffer, int offset, int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("Attempt to write negative bytes");
        }

        if (pos + count > length) {
            throw new IndexOutOfBoundsException("Attempt to write beyond end of substream");
        }

        parent.write(pos + first, buffer, offset, count);
    }

    /**
     * Sets the capacity of the buffer, truncating if appropriate.
     *
     * @param value The desired capacity of the buffer.
     */
    @Override
    public void setCapacity(long value) {
        throw new UnsupportedOperationException("Attempt to change length of a subbuffer");
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
        long absStart = first + start;
        long absEnd = Math.min(absStart + count, first + length);
        return offsetExtents(parent.getExtentsInRange(absStart, absEnd - absStart));
    }

    private List<StreamExtent> offsetExtents(List<StreamExtent> src) {
        return src.stream().map(e -> new StreamExtent(e.getStart() - first, e.getLength())).collect(Collectors.toList());
    }
}
