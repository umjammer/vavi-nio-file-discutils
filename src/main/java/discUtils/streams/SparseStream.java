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
import discUtils.streams.util.Sizes;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


/**
 * Represents a sparse stream. A sparse stream is a logically contiguous stream
 * where some parts of the stream aren't stored. The unstored parts are
 * implicitly zero-byte ranges.
 */
public abstract class SparseStream extends Stream {

    /**
     * Gets the parts of the stream that are stored. This may be an empty
     * enumeration if all bytes are zero.
     */
    public abstract List<StreamExtent> getExtents();

    /**
     * Converts any stream into a sparse stream.
     *
     * The returned stream has the entire wrapped stream as a single extent.
     *
     * @param stream The stream to convert.
     * @param takeOwnership {@code true} to have the new stream dispose the
     *            wrapped stream when it is disposed.
     * @return A sparse stream.
     */
    public static SparseStream fromStream(Stream stream, Ownership takeOwnership) {
        return new SparseWrapperStream(stream, takeOwnership, null);
    }

    /**
     * Converts any stream into a sparse stream.
     *
     * The returned stream has the entire wrapped stream as a single extent.
     *
     * @param stream The stream to convert.
     * @param takeOwnership {@code true} to have the new stream dispose the
     *            wrapped stream when it is disposed.
     * @param extents The set of extents actually stored in {@code stream} .
     * @return A sparse stream.
     */
    public static SparseStream fromStream(Stream stream, Ownership takeOwnership, List<StreamExtent> extents) {
        return new SparseWrapperStream(stream, takeOwnership, extents);
    }

    /**
     * Efficiently pumps data from a sparse stream to another stream.
     *
     * @param inStream The sparse stream to pump from.
     * @param outStream The stream to pump to. {@code outStream} must support
     *            seeking.
     */
    public static void pump(Stream inStream, Stream outStream) {
        pump(inStream, outStream, Sizes.Sector);
    }

    /**
     * Efficiently pumps data from a sparse stream to another stream.
     *
     * {@code outStream} must support seeking.
     * 
     * @param inStream The stream to pump from.
     * @param outStream The stream to pump to.
     * @param chunkSize The smallest sequence of zero bytes that will be skipped
     *            when writing to {@code outStream} .
     */
    public static void pump(Stream inStream, Stream outStream, int chunkSize) {
        StreamPump pump = new StreamPump(inStream, outStream, chunkSize);
        pump.run();
    }

    /**
     * Wraps a sparse stream in a read-only wrapper, preventing modification.
     *
     * @param toWrap The stream to make read-only.
     * @param ownership Whether to transfer responsibility for calling Dispose
     *            on {@code toWrap} .
     * @return The read-only stream.
     */
    public static SparseStream readOnly(SparseStream toWrap, Ownership ownership) {
        return new SparseReadOnlyWrapperStream(toWrap, ownership);
    }

    /**
     * Clears bytes from the stream.
     *
     * Logically equivalent to writing {@code count} null/zero bytes to the
     * stream, some implementations determine that some (or all) of the range
     * indicated is not actually stored. There is no direct, automatic,
     * correspondence to clearing bytes and them not being represented as an
     * 'extent' - for example, the implementation of the underlying stream may
     * not permit fine-grained extent storage.
     *
     * It is always safe to call this method to 'zero-out' a section of a
     * stream, regardless of the underlying stream implementation.
     * 
     * @param count The number of bytes (from the current position) to clear.
     */
    public void clear(int count) {
        write(new byte[count], 0, count);
    }

    /**
     * Gets the parts of a stream that are stored, within a specified range.
     *
     * @param start The offset of the first byte of interest.
     * @param count The number of bytes of interest.
     * @return An enumeration of stream extents, indicating stored bytes.
     */
    public List<StreamExtent> getExtentsInRange(long start, long count) {
        return StreamExtent.intersect(getExtents(), Collections.singletonList(new StreamExtent(start, count)));
    }

    private static class SparseReadOnlyWrapperStream extends SparseStream {

        private final Ownership ownsWrapped;

        private SparseStream wrapped;

        public SparseReadOnlyWrapperStream(SparseStream wrapped, Ownership ownsWrapped) {
            this.wrapped = wrapped;
            this.ownsWrapped = ownsWrapped;
        }

        @Override public boolean canRead() {
            return wrapped.canRead();
        }

        @Override public boolean canSeek() {
            return wrapped.canSeek();
        }

        @Override public boolean canWrite() {
            return false;
        }

        @Override public List<StreamExtent> getExtents() {
            return wrapped.getExtents();
        }

        @Override public long getLength() {
            return wrapped.getLength();
        }

        @Override public long position() {
            return wrapped.position();
        }

        @Override public void position(long value) {
            wrapped.position(value);
        }

        @Override public void flush() {
        }

        @Override public int read(byte[] buffer, int offset, int count) {
            return wrapped.read(buffer, offset, count);
        }

        @Override public long seek(long offset, SeekOrigin origin) {
            return wrapped.seek(offset, origin);
        }

        @Override public void setLength(long value) {
            throw new UnsupportedOperationException("Attempt to change length of read-only stream");
        }

        @Override public void write(byte[] buffer, int offset, int count) {
            throw new UnsupportedOperationException("Attempt to write to read-only stream");
        }

        @Override public void close() throws IOException {
            if (ownsWrapped == Ownership.Dispose && wrapped != null) {
                wrapped.close();
                wrapped = null;
            }
        }
    }

    private static class SparseWrapperStream extends SparseStream {

        private List<StreamExtent> extents;

        private final Ownership ownsWrapped;

        private Stream wrapped;

        public SparseWrapperStream(Stream wrapped, Ownership ownsWrapped, List<StreamExtent> extents) {
            this.wrapped = wrapped;
            this.ownsWrapped = ownsWrapped;
            if (extents != null) {
                this.extents = new ArrayList<>(extents);
            }
        }

        @Override public boolean canRead() {
            return wrapped.canRead();
        }

        @Override public boolean canSeek() {
            return wrapped.canSeek();
        }

        @Override public boolean canWrite() {
            return wrapped.canWrite();
        }

        @Override public List<StreamExtent> getExtents() {
            if (extents != null) {
                return extents;
            }
            if (wrapped instanceof SparseStream) {
                return ((SparseStream) wrapped).getExtents();
            }
            return Collections.singletonList(new StreamExtent(0, wrapped.getLength()));
        }

        @Override public long getLength() {
            return wrapped.getLength();
        }

        @Override public long position() {
            return wrapped.position();
        }

        @Override public void position(long value) {
            wrapped.position(value);
        }

        @Override public void flush() {
        }

        @Override public int read(byte[] buffer, int offset, int count) {
            return wrapped.read(buffer, offset, count);
        }

        @Override public long seek(long offset, SeekOrigin origin) {
            return wrapped.seek(offset, origin);
        }

        @Override public void setLength(long value) {
            wrapped.setLength(value);
        }

        @Override public void write(byte[] buffer, int offset, int count) {
            if (extents != null) {
                throw new UnsupportedOperationException("Attempt to write to stream with explicit extents");
            }

            wrapped.write(buffer, offset, count);
        }

        @Override public void close() throws IOException {
            if (ownsWrapped == Ownership.Dispose && wrapped != null) {
                wrapped.close();
                wrapped = null;
            }
        }
    }
}
