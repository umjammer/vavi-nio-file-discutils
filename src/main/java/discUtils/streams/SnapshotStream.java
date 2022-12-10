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

    private Stream baseStream;

    private final Ownership baseStreamOwnership;

    /**
     * Records which byte ranges in diffStream hold changes.
     * Can't use diffStream's own tracking because that's based on it's
     * internal block size, not on the _actual_ bytes stored.
     */
    private List<StreamExtent> diffExtents;

    /**
     * Captures changes to the base stream (when enabled).
     */
    private SparseMemoryStream diffStream;

    /**
     * Indicates that no writes should be permitted.
     */
    private boolean frozen;

    private long position;

    /**
     * The saved stream position (if the diffStream is active).
     */
    private long savedPosition;

    /**
     * Initializes a new instance of the SnapshotStream class.
     *
     * @param baseStream The stream to wrap.
     * @param owns Indicates if this stream should control the lifetime of
     *            baseStream.
     */
    public SnapshotStream(Stream baseStream, Ownership owns) {
        this.baseStream = baseStream;
        baseStreamOwnership = owns;
        diffExtents = new ArrayList<>();
    }

    /**
     * Gets an indication as to whether the stream can be read.
     */
    @Override public boolean canRead() {
        return baseStream.canRead();
    }

    /**
     * Gets an indication as to whether the stream position can be changed.
     */
    @Override public boolean canSeek() {
        return baseStream.canSeek();
    }

    /**
     * Gets an indication as to whether the stream can be written to.
     * This property is orthogonal to Freezing/Thawing, it's
     * perfectly possible for a stream to be frozen and this method
     * return
     * {@code true}
     * .
     */
    @Override public boolean canWrite() {
        return diffStream != null ? true : baseStream.canWrite();
    }

    /**
     * Returns an enumeration over the parts of the stream that contain real
     * data.
     */
    @Override public List<StreamExtent> getExtents() {
        if (baseStream instanceof SparseStream) {
            return StreamExtent.union(((SparseStream) baseStream).getExtents(), diffExtents);
        }
        return Collections.singletonList(new StreamExtent(0, getLength()));
    }

    /**
     * Gets the length of the stream.
     */
    @Override public long getLength() {
        if (diffStream != null) {
            return diffStream.getLength();
        }

        return baseStream.getLength();
    }

    /**
     * Gets and sets the current stream position.
     */
    @Override public long position() {
        return position;
    }

    @Override public void position(long value) {
        position = value;
    }

    /**
     * Prevents any write operations to the stream.
     * Useful to prevent changes whilst inspecting the stream.
     */
    public void freeze() {
        frozen = true;
    }

    /**
     * Re-permits write operations to the stream.
     */
    public void thaw() {
        frozen = false;
    }

    /**
     * Takes a snapshot of the current stream contents.
     */
    public void snapshot() {
        if (diffStream != null) {
            throw new IllegalStateException("Already have a snapshot");
        }

        savedPosition = position;
        diffExtents = new ArrayList<>();
        diffStream = new SparseMemoryStream();
        diffStream.setLength(baseStream.getLength());
    }

    /**
     * Reverts to a previous snapshot, discarding any changes made to the
     * stream.
     */
    public void revertToSnapshot() {
        if (diffStream == null) {
            throw new IllegalStateException("No snapshot");
        }

        diffStream = null;
        diffExtents = null;
        position = savedPosition;
    }

    /**
     * Discards the snapshot any changes made after the snapshot was taken are
     * kept.
     */
    public void forgetSnapshot() {
        if (diffStream == null) {
            throw new IllegalStateException("No snapshot");
        }

        byte[] buffer = new byte[8192];
        for (StreamExtent extent : diffExtents) {
            diffStream.position(extent.getStart());
            baseStream.position(extent.getStart());
            int totalRead = 0;
            while (totalRead < extent.getLength()) {
                int toRead = (int) Math.min(extent.getLength() - totalRead, buffer.length);
                int read = diffStream.read(buffer, 0, toRead);
                baseStream.write(buffer, 0, read);
                totalRead += read;
            }
        }
        diffStream = null;
        diffExtents = null;
    }

    /**
     * Flushes the stream.
     */
    @Override public void flush() {
        checkFrozen();
        baseStream.flush();
    }

    /**
     * Reads data from the stream.
     *
     * @param buffer The buffer to fill.
     * @param offset The buffer offset to start from.
     * @param count The number of bytes to read.
     * @return The number of bytes read.
     */
    @Override public int read(byte[] buffer, int offset, int count) {
        int numRead;

        if (diffStream == null) {
            baseStream.position(position);
            numRead = baseStream.read(buffer, offset, count);
        } else {
            if (position > diffStream.getLength()) {
                throw new dotnet4j.io.IOException("Attempt to read beyond end of file");
            }

            int toRead = (int) Math.min(count, diffStream.getLength() - position);

            // If the read is within the base stream's range, then touch it first to get the
            // (potentially) stale data.
            if (position < baseStream.getLength()) {
                int baseToRead = (int) Math.min(toRead, baseStream.getLength() - position);
                baseStream.position(position);

                int totalBaseRead = 0;
                while (totalBaseRead < baseToRead) {
                    totalBaseRead += baseStream.read(buffer, offset + totalBaseRead, baseToRead - totalBaseRead);
                }
            }

            // Now overlay any data from the overlay stream (if any)
            List<StreamExtent> overlayExtents = StreamExtent.intersect(diffExtents, new StreamExtent(position, toRead));
            for (StreamExtent extent : overlayExtents) {
                diffStream.position(extent.getStart());
                int overlayNumRead = 0;
                while (overlayNumRead < extent.getLength()) {
                    overlayNumRead += diffStream.read(buffer,
                                                       (int) (offset + (extent.getStart() - position) + overlayNumRead),
                                                       (int) (extent.getLength() - overlayNumRead));
                }
            }

            numRead = toRead;
        }

        position += numRead;

        return numRead;
    }

    /**
     * Moves the stream position.
     *
     * @param offset The origin-relative location.
     * @param origin The base location.
     * @return The new absolute stream position.
     */
    @Override public long seek(long offset, SeekOrigin origin) {
        checkFrozen();
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
     * Sets the length of the stream.
     *
     * @param value The new length.
     */
    @Override public void setLength(long value) {
        checkFrozen();
        if (diffStream != null) {
            diffStream.setLength(value);
        } else {
            baseStream.setLength(value);
        }
    }

    /**
     * Writes data to the stream at the current location.
     *
     * @param buffer The data to write.
     * @param offset The first byte to write from buffer.
     * @param count The number of bytes to write.
     */
    @Override public void write(byte[] buffer, int offset, int count) {
        checkFrozen();

        if (diffStream != null) {
            diffStream.position(position);
            diffStream.write(buffer, offset, count);

            // Beware of Linq's delayed model - force execution now by placing into a list.
            // Without this, large execution chains can build up (v. slow) and potential for stack overflow.
            diffExtents = new ArrayList<>(StreamExtent.union(diffExtents, new StreamExtent(position, count)));

            position += count;
        } else {
            baseStream.position(position);
            baseStream.write(buffer, offset, count);
            position += count;
        }
    }

    /**
     * Disposes of this instance.
     */
    @Override public void close() throws IOException {
        if (baseStreamOwnership == Ownership.Dispose && baseStream != null) {
            baseStream.close();
        }

        baseStream = null;
        if (diffStream != null) {
            diffStream.close();
        }

        diffStream = null;
    }

    private void checkFrozen() {
        if (frozen) {
            throw new UnsupportedOperationException("The stream is frozen");
        }
    }
}
