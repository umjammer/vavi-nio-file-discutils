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
import java.util.stream.Collectors;

import DiscUtils.Streams.StreamExtent;


/**
 * Class representing a portion of an existing buffer.
 */
public class SubBuffer extends Buffer {
    private final long _first;

    private final long _length;

    private final IBuffer _parent;

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
        _parent = parent;
        _first = first;
        _length = length;
        if (_first + _length > _parent.getCapacity()) {
            throw new IllegalArgumentException("Substream extends beyond end of parent stream");
        }

    }

    /**
     * Can this buffer be read.
     */
    public boolean canRead() {
        return _parent.canRead();
    }

    /**
     * Can this buffer be modified.
     */
    public boolean canWrite() {
        return _parent.canWrite();
    }

    /**
     * Gets the current capacity of the buffer, in bytes.
     */
    public long getCapacity() {
        return _length;
    }

    /**
     * Gets the parts of the buffer that are stored.
     * This may be an empty enumeration if all bytes are zero.
     */
    public List<StreamExtent> getExtents() {
        return offsetExtents(_parent.getExtentsInRange(_first, _length));
    }

    /**
     * Flushes all data to the underlying storage.
     */
    public void flush() {
        _parent.flush();
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
    public int read(long pos, byte[] buffer, int offset, int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("Attempt to read negative bytes");
        }

        if (pos >= _length) {
            return 0;
        }

        return _parent.read(pos + _first, buffer, offset, (int) Math.min(count, Math.min(_length - pos, Integer.MAX_VALUE)));
    }

    /**
     * Writes a byte array into the buffer.
     *
     * @param pos The start offset within the buffer.
     * @param buffer The source byte array.
     * @param offset The start offset within the source byte array.
     * @param count The number of bytes to write.
     */
    public void write(long pos, byte[] buffer, int offset, int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("Attempt to write negative bytes");
        }

        if (pos + count > _length) {
            throw new IndexOutOfBoundsException("Attempt to write beyond end of substream");
        }

        _parent.write(pos + _first, buffer, offset, count);
    }

    /**
     * Sets the capacity of the buffer, truncating if appropriate.
     *
     * @param value The desired capacity of the buffer.
     */
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
    public List<StreamExtent> getExtentsInRange(long start, long count) {
        long absStart = _first + start;
        long absEnd = Math.min(absStart + count, _first + _length);
        return offsetExtents(_parent.getExtentsInRange(absStart, absEnd - absStart));
    }

    private List<StreamExtent> offsetExtents(List<StreamExtent> src) {
        return src.stream().map(e -> new StreamExtent(e.getStart() - _first, e.getLength())).collect(Collectors.toList());
    }

}
