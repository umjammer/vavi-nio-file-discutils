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

import java.io.IOException;
import java.util.List;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.SeekOrigin;


/**
 * Converts a buffer into a Stream.
 */
public class BufferStream extends SparseStream {

    private final FileAccess access;

    private final IBuffer buffer;

    private long position;

    /**
     * Initializes a new instance of the BufferStream class.
     *
     * @param buffer The buffer to use.
     * @param access The access permitted to clients.
     */
    public BufferStream(IBuffer buffer, FileAccess access) {
        this.buffer = buffer;
        this.access = access;
    }

    /**
     * Gets an indication of whether read access is permitted.
     */
    public boolean canRead() {
        return access != FileAccess.Write;
    }

    /**
     * Gets an indication of whether seeking is permitted.
     */
    public boolean canSeek() {
        return true;
    }

    /**
     * Gets an indication of whether write access is permitted.
     */
    public boolean canWrite() {
        return access != FileAccess.Read;
    }

    /**
     * Gets the stored extents within the sparse stream.
     */
    public List<StreamExtent> getExtents() {
        return buffer.getExtents();
    }

    /**
     * Gets the length of the stream (the capacity of the underlying buffer).
     */
    public long getLength() {
        return buffer.getCapacity();
    }

    /**
     * Gets and sets the current position within the stream.
     */
    public long getPosition() {
        return position;
    }

    public void setPosition(long value) {
        position = value;
    }

    /**
     * Flushes all data to the underlying storage.
     */
    public void flush() {
    }

    /**
     * Reads a number of bytes from the stream.
     *
     * @param buffer The destination buffer.
     * @param offset The start offset within the destination buffer.
     * @param count The number of bytes to read.
     * @return The number of bytes read.
     */
    public int read(byte[] buffer, int offset, int count) {
        if (!canRead()) {
            throw new dotnet4j.io.IOException("Attempt to read from write-only stream");
        }

        StreamUtilities.assertBufferParameters(buffer, offset, count);
        int numRead = this.buffer.read(position, buffer, offset, count);
        position += numRead;
        return numRead;
    }

    /**
     * Changes the current stream position.
     *
     * @param offset The origin-relative stream position.
     * @param origin The origin for the stream position.
     * @return The new stream position.
     */
    public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += buffer.getCapacity();
        }

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
        }

        position = effectiveOffset;
        return position;
    }

    /**
     * Sets the length of the stream (the underlying buffer's capacity).
     *
     * @param value The new length of the stream.
     */
    public void setLength(long value) {
        buffer.setCapacity(value);
    }

    /**
     * Writes a buffer to the stream.
     *
     * @param buffer The buffer to write.
     * @param offset The starting offset within buffer.
     * @param count The number of bytes to write.
     */
    public void write(byte[] buffer, int offset, int count) {
        if (!canWrite()) {
            throw new dotnet4j.io.IOException("Attempt to write to read-only stream");
        }

        StreamUtilities.assertBufferParameters(buffer, offset, count);
        this.buffer.write(position, buffer, offset, count);
        position += count;
    }

    /**
     * Clears bytes from the stream.
     *
     * Logically equivalent to writing {@code count} null/zero bytes to the
     * stream, some implementations determine that some (or all) of the range
     * indicated is not actually stored. There is no direct, automatic,
     * correspondence to clearing bytes and them not being represented as an
     * 'extent' - for example, the implementation of the underlying stream may
     * not permit fine-grained extent storage.It is always safe to call this
     * method to 'zero-out' a section of a stream, regardless of the underlying
     * stream implementation.
     * 
     * @param count The number of bytes (from the current position) to clear.
     */
    public void clear(int count) {
        if (!canWrite()) {
            throw new dotnet4j.io.IOException("Attempt to erase bytes in a read-only stream");
        }

        buffer.clear(position, count);
        position += count;
    }

    /**
     * Gets the parts of a stream that are stored, within a specified range.
     *
     * @param start The offset of the first byte of interest.
     * @param count The number of bytes of interest.
     * @return An enumeration of stream extents, indicating stored bytes.
     */
    public List<StreamExtent> getExtentsInRange(long start, long count) {
        return buffer.getExtentsInRange(start, count);
    }

    @Override
    public void close() throws IOException {
    }
}
