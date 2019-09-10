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

import java.io.IOException;
import java.util.List;

import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.SeekOrigin;


/**
 * Provides a thread-safe wrapping around a sparse stream.
 * Streams are inherently not thread-safe (because read/write is not atomic
 * w.r.t. Position).
 * This method enables multiple 'views' of a stream to be created (each with
 * their own Position), and ensures
 * only a single operation is executing on the wrapped stream at any time.This
 * example shows the pattern of use:
 * {@code 
* SparseStream baseStream = ...;
* ThreadSafeStream tss = new ThreadSafeStream(baseStream);
* for(int i = 0; i < 10; ++i)
* {
* SparseStream streamForThread = tss.OpenView();
 * }
 * }
 * This results in 11 streams that can be used in different streams -
 * {@code tss}
 * and ten 'views' created from
 * {@code tss}
 * .Note, the stream length cannot be changed.
 */
public class ThreadSafeStream extends SparseStream {
    private CommonState _common;

    private boolean _ownsCommon;

    private long _position;

    /**
     * Initializes a new instance of the ThreadSafeStream class.
     * 
     * @param toWrap The stream to wrap.Do not directly modify
     *            {@code toWrap}
     *            after wrapping it, unless the thread-safe views
     *            will no longer be used.
     */
    public ThreadSafeStream(SparseStream toWrap) {
        this(toWrap, Ownership.None);
    }

    /**
     * Initializes a new instance of the ThreadSafeStream class.
     * 
     * @param toWrap The stream to wrap.
     * @param ownership Whether to transfer ownership of
     *            {@code toWrap}
     *            to the new instance.Do not directly modify
     *            {@code toWrap}
     *            after wrapping it, unless the thread-safe views
     *            will no longer be used.
     */
    public ThreadSafeStream(SparseStream toWrap, Ownership ownership) {
        if (!toWrap.canSeek()) {
            throw new IllegalArgumentException("Wrapped stream must support seeking");
        }

        _common = new CommonState();
        _ownsCommon = true;
    }

    private ThreadSafeStream(ThreadSafeStream toClone) {
        _common = toClone._common;
        if (_common == null) {
            throw new moe.yo3explorer.dotnetio4j.IOException("toClone");
        }
    }

    /**
     * Gets a value indicating if this stream supports reads.
     */
    public boolean canRead() {
        synchronized (_common) {
            {
                return getWrapped().canRead();
            }
        }
    }

    /**
     * Gets a value indicating if this stream supports seeking (always true).
     */
    public boolean canSeek() {
        return true;
    }

    /**
     * Gets a value indicating if this stream supports writes (currently, always
     * false).
     */
    public boolean canWrite() {
        synchronized (_common) {
            {
                return getWrapped().canWrite();
            }
        }
    }

    /**
     * Gets the parts of the stream that are stored.
     * This may be an empty enumeration if all bytes are zero.
     */
    public List<StreamExtent> getExtents() {
        synchronized (_common) {
            {
                return getWrapped().getExtents();
            }
        }
    }

    /**
     * Gets the length of the stream.
     */
    public long getLength() {
        synchronized (_common) {
            return getWrapped().getLength();
        }
    }

    /**
     * Gets the current stream position - each 'view' has it's own Position.
     */
    public long getPosition() {
        return _position;
    }

    public void setPosition(long value) {
        _position = value;
    }

    private SparseStream getWrapped() {
        SparseStream wrapped = _common.WrappedStream;
        if (wrapped == null) {
            throw new moe.yo3explorer.dotnetio4j.IOException("ThreadSafeStream");
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
    public List<StreamExtent> getExtentsInRange(long start, long count) {
        synchronized (_common) {
            return getWrapped().getExtentsInRange(start, count);
        }
    }

    /**
     * Causes the stream to flush all changes.
     */
    public void flush() {
        synchronized (_common) {
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
    public int read(byte[] buffer, int offset, int count) {
        synchronized (_common) {
            SparseStream wrapped = getWrapped();
            wrapped.setPosition(_position);
            int numRead = wrapped.read(buffer, offset, count);
            _position += numRead;
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
    public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += _position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += getLength();
        }

        if (effectiveOffset < 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to move before beginning of disk");
        }

        _position = effectiveOffset;
        return _position;
    }

    /**
     * Sets the length of the stream (not supported).
     * 
     * @param value The new length.
     */
    public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes data to the stream (not currently supported).
     * 
     * @param buffer The data to write.
     * @param offset The first byte to write.
     * @param count The number of bytes to write.
     */
    public void write(byte[] buffer, int offset, int count) {
        synchronized (_common) {
            SparseStream wrapped = getWrapped();
            if (_position + count > wrapped.getLength()) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to extend stream");
            }

            wrapped.setPosition(_position);
            wrapped.write(buffer, offset, count);
            _position += count;
        }
    }

    /**
     * Disposes of this instance, invalidating any remaining views.
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        if (_ownsCommon && _common != null) {
            synchronized (_common) {
                if (_common.WrappedStreamOwnership == Ownership.Dispose) {
                    _common.WrappedStream.close();
                }

                _common.close();
            }
        }
        _common = null;
    }

    private final static class CommonState implements Cloneable {
        public SparseStream WrappedStream;

        public Ownership WrappedStreamOwnership = Ownership.None;

        public void close() {
            WrappedStream = null;
        }
    }
}
