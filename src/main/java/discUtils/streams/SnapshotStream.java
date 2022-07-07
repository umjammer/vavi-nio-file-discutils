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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import discUtils.streams.util.Ownership;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


/**
 * A wrapper stream that enables you to take a snapshot, pushing changes into a
 * side buffer.
 * Once a snapshot is taken, you can discard subsequent changes or merge them
 * back
 * into the wrapped stream.
 */
public final class SnapshotStream extends SparseStream {
    private Stream _baseStream;

    private final Ownership _baseStreamOwnership;

    /**
     * Records which byte ranges in diffStream hold changes.
     * Can't use _diffStream's own tracking because that's based on it's
     * internal block size, not on the _actual_ bytes stored.
     */
    private List<StreamExtent> _diffExtents;

    /**
     * Captures changes to the base stream (when enabled).
     */
    private SparseMemoryStream _diffStream;

    /**
     * Indicates that no writes should be permitted.
     */
    private boolean _frozen;

    private long _position;

    /**
     * The saved stream position (if the diffStream is active).
     */
    private long _savedPosition;

    /**
     * Initializes a new instance of the SnapshotStream class.
     *
     * @param baseStream The stream to wrap.
     * @param owns Indicates if this stream should control the lifetime of
     *            baseStream.
     */
    public SnapshotStream(Stream baseStream, Ownership owns) {
        _baseStream = baseStream;
        _baseStreamOwnership = owns;
        _diffExtents = new ArrayList<>();
    }

    /**
     * Gets an indication as to whether the stream can be read.
     */
    public boolean canRead() {
        return _baseStream.canRead();
    }

    /**
     * Gets an indication as to whether the stream position can be changed.
     */
    public boolean canSeek() {
        return _baseStream.canSeek();
    }

    /**
     * Gets an indication as to whether the stream can be written to.
     * This property is orthogonal to Freezing/Thawing, it's
     * perfectly possible for a stream to be frozen and this method
     * return
     * {@code true}
     * .
     */
    public boolean canWrite() {
        return _diffStream != null ? true : _baseStream.canWrite();
    }

    /**
     * Returns an enumeration over the parts of the stream that contain real
     * data.
     */
    public List<StreamExtent> getExtents() {
        if (_baseStream instanceof SparseStream) {
            return StreamExtent.union(((SparseStream) _baseStream).getExtents(), _diffExtents);
        }
        return Collections.singletonList(new StreamExtent(0, getLength()));
    }

    /**
     * Gets the length of the stream.
     */
    public long getLength() {
        if (_diffStream != null) {
            return _diffStream.getLength();
        }

        return _baseStream.getLength();
    }

    /**
     * Gets and sets the current stream position.
     */
    public long getPosition() {
        return _position;
    }

    public void setPosition(long value) {
        _position = value;
    }

    /**
     * Prevents any write operations to the stream.
     * Useful to prevent changes whilst inspecting the stream.
     */
    public void freeze() {
        _frozen = true;
    }

    /**
     * Re-permits write operations to the stream.
     */
    public void thaw() {
        _frozen = false;
    }

    /**
     * Takes a snapshot of the current stream contents.
     */
    public void snapshot() {
        if (_diffStream != null) {
            throw new IllegalStateException("Already have a snapshot");
        }

        _savedPosition = _position;
        _diffExtents = new ArrayList<>();
        _diffStream = new SparseMemoryStream();
        _diffStream.setLength(_baseStream.getLength());
    }

    /**
     * Reverts to a previous snapshot, discarding any changes made to the
     * stream.
     */
    public void revertToSnapshot() {
        if (_diffStream == null) {
            throw new IllegalStateException("No snapshot");
        }

        _diffStream = null;
        _diffExtents = null;
        _position = _savedPosition;
    }

    /**
     * Discards the snapshot any changes made after the snapshot was taken are
     * kept.
     */
    public void forgetSnapshot() {
        if (_diffStream == null) {
            throw new IllegalStateException("No snapshot");
        }

        byte[] buffer = new byte[8192];
        for (StreamExtent extent : _diffExtents) {
            _diffStream.setPosition(extent.getStart());
            _baseStream.setPosition(extent.getStart());
            int totalRead = 0;
            while (totalRead < extent.getLength()) {
                int toRead = (int) Math.min(extent.getLength() - totalRead, buffer.length);
                int read = _diffStream.read(buffer, 0, toRead);
                _baseStream.write(buffer, 0, read);
                totalRead += read;
            }
        }
        _diffStream = null;
        _diffExtents = null;
    }

    /**
     * Flushes the stream.
     */
    public void flush() {
        checkFrozen();
        _baseStream.flush();
    }

    /**
     * Reads data from the stream.
     *
     * @param buffer The buffer to fill.
     * @param offset The buffer offset to start from.
     * @param count The number of bytes to read.
     * @return The number of bytes read.
     */
    public int read(byte[] buffer, int offset, int count) {
        int numRead;

        if (_diffStream == null) {
            _baseStream.setPosition(_position);
            numRead = _baseStream.read(buffer, offset, count);
        } else {
            if (_position > _diffStream.getLength()) {
                throw new dotnet4j.io.IOException("Attempt to read beyond end of file");
            }

            int toRead = (int) Math.min(count, _diffStream.getLength() - _position);

            // If the read is within the base stream's range, then touch it first to get the
            // (potentially) stale data.
            if (_position < _baseStream.getLength()) {
                int baseToRead = (int) Math.min(toRead, _baseStream.getLength() - _position);
                _baseStream.setPosition(_position);

                int totalBaseRead = 0;
                while (totalBaseRead < baseToRead) {
                    totalBaseRead += _baseStream.read(buffer, offset + totalBaseRead, baseToRead - totalBaseRead);
                }
            }

            // Now overlay any data from the overlay stream (if any)
            List<StreamExtent> overlayExtents = StreamExtent.intersect(_diffExtents, new StreamExtent(_position, toRead));
            for (StreamExtent extent : overlayExtents) {
                _diffStream.setPosition(extent.getStart());
                int overlayNumRead = 0;
                while (overlayNumRead < extent.getLength()) {
                    overlayNumRead += _diffStream.read(buffer,
                                                       (int) (offset + (extent.getStart() - _position) + overlayNumRead),
                                                       (int) (extent.getLength() - overlayNumRead));
                }
            }

            numRead = toRead;
        }

        _position += numRead;

        return numRead;
    }

    /**
     * Moves the stream position.
     *
     * @param offset The origin-relative location.
     * @param origin The base location.
     * @return The new absolute stream position.
     */
    public long seek(long offset, SeekOrigin origin) {
        checkFrozen();
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += _position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += getLength();
        }

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
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
        checkFrozen();
        if (_diffStream != null) {
            _diffStream.setLength(value);
        } else {
            _baseStream.setLength(value);
        }
    }

    /**
     * Writes data to the stream at the current location.
     *
     * @param buffer The data to write.
     * @param offset The first byte to write from buffer.
     * @param count The number of bytes to write.
     */
    public void write(byte[] buffer, int offset, int count) {
        checkFrozen();

        if (_diffStream != null) {
            _diffStream.setPosition(_position);
            _diffStream.write(buffer, offset, count);

            // Beware of Linq's delayed model - force execution now by placing into a list.
            // Without this, large execution chains can build up (v. slow) and potential for stack overflow.
            _diffExtents = new ArrayList<>(StreamExtent.union(_diffExtents, new StreamExtent(_position, count)));

            _position += count;
        } else {
            _baseStream.setPosition(_position);
            _baseStream.write(buffer, offset, count);
            _position += count;
        }
    }

    /**
     * Disposes of this instance.
     */
    public void close() throws IOException {
        if (_baseStreamOwnership == Ownership.Dispose && _baseStream != null) {
            _baseStream.close();
        }

        _baseStream = null;
        if (_diffStream != null) {
            _diffStream.close();
        }

        _diffStream = null;
    }

    private void checkFrozen() {
        if (_frozen) {
            throw new UnsupportedOperationException("The stream is frozen");
        }
    }
}
