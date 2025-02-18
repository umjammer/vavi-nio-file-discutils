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

import java.io.IOException;
import java.util.List;

import discUtils.streams.util.Ownership;
import dotnet4j.io.SeekOrigin;


/**
 * Provides a thread-safe wrapping around a sparse stream.
 * <p>
 * streams are inherently not thread-safe (because read/write is not atomic
 * w.r.t. Position). This method enables multiple 'views' of a stream to be
 * created (each with their own Position), and ensures only a single operation
 * is executing on the wrapped stream at any time.
 *
 * This example shows the pattern of use:
 *
 * <pre>
 * {@code
 *  SparseStream baseStream = ...;
 *  ThreadSafeStream tss = new ThreadSafeStream(baseStream);
 *  for (int i = 0; i < 10; ++i) {
 *    SparseStream streamForThread = tss.OpenView();
 *  }
 * }
 * </pre>
 *
 * This results in 11 streams that can be used in different streams -
 * {@code tss} and ten 'views' created from {@code tss} .
 * <p>
 * Note, the stream length cannot be changed.
 */
public class ThreadSafeStream extends SparseStream {

    private final CommonState common;

    private boolean ownsCommon;

    private long position;

    /**
     * Initializes a new instance of the ThreadSafeStream class.
     *
     * Do not directly modify {@code toWrap} after wrapping it, unless the
     * thread-safe views will no longer be used.
     *
     * @param toWrap The stream to wrap.
     */
    public ThreadSafeStream(SparseStream toWrap) {
        this(toWrap, Ownership.None);
    }

    /**
     * Initializes a new instance of the ThreadSafeStream class.
     *
     * Do not directly modify {@code toWrap} after wrapping it, unless the
     * thread-safe views will no longer be used.
     *
     * @param toWrap The stream to wrap.
     * @param ownership Whether to transfer ownership of {@code toWrap} to the
     *            new instance.
     */
    public ThreadSafeStream(SparseStream toWrap, Ownership ownership) {
        if (!toWrap.canSeek()) {
            throw new IllegalArgumentException("Wrapped stream must support seeking");
        }

        common = new CommonState();
        common.wrappedStream = toWrap;
        common.wrappedStreamOwnership = ownership;
        ownsCommon = true;
    }

    private ThreadSafeStream(ThreadSafeStream toClone) {
        common = toClone.common;
        if (common == null) {
            throw new dotnet4j.io.IOException("toClone");
        }
    }

    /**
     * Gets a value indicating if this stream supports reads.
     */
    @Override public boolean canRead() {
        synchronized (common) {
            return getWrapped().canRead();
        }
    }

    /**
     * Gets a value indicating if this stream supports seeking (always true).
     */
    @Override public boolean canSeek() {
        return true;
    }

    /**
     * Gets a value indicating if this stream supports writes (currently, always
     * false).
     */
    @Override public boolean canWrite() {
        synchronized (common) {
            return getWrapped().canWrite();
        }
    }

    /**
     * Gets the parts of the stream that are stored. This may be an empty
     * enumeration if all bytes are zero.
     */
    @Override public List<StreamExtent> getExtents() {
        synchronized (common) {
            return getWrapped().getExtents();
        }
    }

    /**
     * Gets the length of the stream.
     */
    @Override public long getLength() {
        synchronized (common) {
            return getWrapped().getLength();
        }
    }

    /**
     * Gets the current stream position - each 'view' has it's own Position.
     */
    @Override public long position() {
        return position;
    }

    @Override public void position(long value) {
        position = value;
    }

    private SparseStream getWrapped() {
        SparseStream wrapped = common.wrappedStream;
        if (wrapped == null) {
            throw new dotnet4j.io.IOException("no wrapped stream.");
        }

        return wrapped;
    }

    /**
     * Opens a new thread-safe view on the stream.
     *
     * @return The new view.
     */
    public SparseStream openView() {
        return new ThreadSafeStream(this);
    }

    /**
     * Gets the parts of a stream that are stored, within a specified range.
     *
     * @param start The offset of the first byte of interest.
     * @param count The number of bytes of interest.
     * @return An enumeration of stream extents, indicating stored bytes.
     */
    @Override public List<StreamExtent> getExtentsInRange(long start, long count) {
        synchronized (common) {
            return getWrapped().getExtentsInRange(start, count);
        }
    }

    /**
     * Causes the stream to flush all changes.
     */
    @Override
    public void flush() {
        synchronized (common) {
            getWrapped().flush();
        }
    }

    /**
     * Reads data from the stream.
     *
     * @param buffer The buffer to fill.
     * @param offset The first byte in buffer to fill.
     * @param count The requested number of bytes to read.
     * @return The actual number of bytes read.
     */
    @Override public int read(byte[] buffer, int offset, int count) {
        synchronized (common) {
            SparseStream wrapped = getWrapped();
            wrapped.position(position);
            int numRead = wrapped.read(buffer, offset, count);
            position += numRead;
            return numRead;
        }
    }

    /**
     * Changes the current stream position (each view has it's own Position).
     *
     * @param offset The relative location to move to.
     * @param origin The origin of the location.
     * @return The new location as an absolute position.
     */
    @Override public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += getLength();
        }

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
        }

        position = effectiveOffset;
        return position;
    }

    /**
     * Sets the length of the stream (not supported).
     *
     * @param value The new length.
     */
    @Override public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes data to the stream (not currently supported).
     *
     * @param buffer The data to write.
     * @param offset The first byte to write.
     * @param count The number of bytes to write.
     */
    @Override public void write(byte[] buffer, int offset, int count) {
        synchronized (common) {
            SparseStream wrapped = getWrapped();
            if (position + count > wrapped.getLength()) {
                throw new dotnet4j.io.IOException("Attempt to extend stream");
            }

            wrapped.position(position);
            wrapped.write(buffer, offset, count);
            position += count;
        }
    }

    /**
     * Disposes of this instance, invalidating any remaining views.
     *
     * @throws IOException when an io error occurs
     */
    @Override public void close() throws IOException {
        if (ownsCommon && common != null) {
            synchronized (common) {
                if (common.wrappedStreamOwnership == Ownership.Dispose) {
                    common.wrappedStream.close();
                }

                common.close();
            }
        }
    }

    private final static class CommonState implements Cloneable {
        public SparseStream wrappedStream;

        public Ownership wrappedStreamOwnership = Ownership.None;

        public void close() {
            wrappedStream = null;
        }
    }
}
