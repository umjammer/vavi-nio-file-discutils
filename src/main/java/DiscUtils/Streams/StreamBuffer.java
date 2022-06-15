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

package DiscUtils.Streams;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import DiscUtils.Streams.Buffer.Buffer;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.Stream;


/**
 * Converts a Stream into an IBuffer instance.
 */
public final class StreamBuffer extends Buffer implements Closeable {
    private final Ownership _ownership;

    private SparseStream _stream;

    /**
     * Initializes a new instance of the StreamBuffer class.
     *
     * @param stream The stream to wrap.
     * @param ownership Whether to dispose stream, when this object is disposed.
     */
    public StreamBuffer(Stream stream, Ownership ownership) {
        if (stream == null) {
            throw new IllegalArgumentException("stream");
        }

        _stream = stream instanceof SparseStream ? (SparseStream) stream : null;
        if (_stream == null) {
            _stream = SparseStream.fromStream(stream, ownership);
            _ownership = Ownership.Dispose;
        } else {
            _ownership = ownership;
        }
    }

    /**
     * Can this buffer be read.
     */
    public boolean canRead() {
        return _stream.canRead();
    }

    /**
     * Can this buffer be written.
     */
    public boolean canWrite() {
        return _stream.canWrite();
    }

    /**
     * Gets the current capacity of the buffer, in bytes.
     */
    public long getCapacity() {
        return _stream.getLength();
    }

    /**
     * Gets the parts of the stream that are stored.
     * This may be an empty enumeration if all bytes are zero.
     */
    public List<StreamExtent> getExtents() {
        return _stream.getExtents();
    }

    /**
     * Disposes of this instance.
     */
    public void close() throws IOException {
        if (_ownership == Ownership.Dispose) {
            if (_stream != null) {
                _stream.close();
                _stream = null;
            }
        }
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
        _stream.setPosition(pos);
        return _stream.read(buffer, offset, count);
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
        _stream.setPosition(pos);
        _stream.write(buffer, offset, count);
    }

    /**
     * Flushes all data to the underlying storage.
     */
    public void flush() {
        _stream.flush();
    }

    /**
     * Sets the capacity of the buffer, truncating if appropriate.
     *
     * @param value The desired capacity of the buffer.
     */
    public void setCapacity(long value) {
        _stream.setLength(value);
    }

    /**
     * Gets the parts of a buffer that are stored, within a specified range.
     *
     * @param start The offset of the first byte of interest.
     * @param count The number of bytes of interest.
     * @return An enumeration of stream extents, indicating stored bytes.
     */
    public List<StreamExtent> getExtentsInRange(long start, long count) {
        return _stream.getExtentsInRange(start, count);
    }
}
