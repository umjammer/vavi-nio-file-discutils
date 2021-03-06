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

import java.util.function.BiConsumer;

import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.Stream;

/**
 * Utility class for pumping the contents of one stream into another.
 *
 * This class is aware of sparse streams, and will avoid copying data that is
 * not
 * valid in the source stream. This functionality should normally only be used
 * when the destination stream is known not to contain any existing data.
 */
public final class StreamPump {
    /**
     * Initializes a new instance of the StreamPump class.
     */
    public StreamPump() {
        setSparseChunkSize(512);
        setBufferSize((int) (512 * Sizes.OneKiB));
        setSparseCopy(true);
    }

    /**
     * Initializes a new instance of the StreamPump class.
     *
     * @param inStream The stream to read from.
     * @param outStream The stream to write to.
     * @param sparseChunkSize The size of each sparse chunk.
     */
    public StreamPump(Stream inStream, Stream outStream, int sparseChunkSize) {
        setInputStream(inStream);
        setOutputStream(outStream);
        setSparseChunkSize(sparseChunkSize);
        setBufferSize((int) (512 * Sizes.OneKiB));
        setSparseCopy(true);
    }

    /**
     * Gets or sets the amount of data to read at a time from
     * {@code InputStream}
     * .
     */
    private int __BufferSize;

    public int getBufferSize() {
        return __BufferSize;
    }

    public void setBufferSize(int value) {
        __BufferSize = value;
    }

    /**
     * Gets the number of bytes read from
     * {@code InputStream}
     * .
     */
    private long __BytesRead;

    public long getBytesRead() {
        return __BytesRead;
    }

    public void setBytesRead(long value) {
        __BytesRead = value;
    }

    /**
     * Gets the number of bytes written to
     * {@code OutputStream}
     * .
     */
    private long __BytesWritten;

    public long getBytesWritten() {
        return __BytesWritten;
    }

    public void setBytesWritten(long value) {
        __BytesWritten = value;
    }

    /**
     * Gets or sets the stream that will be read from.
     */
    private Stream __InputStream;

    public Stream getInputStream() {
        return __InputStream;
    }

    public void setInputStream(Stream value) {
        __InputStream = value;
    }

    /**
     * Gets or sets the stream that will be written to.
     */
    private Stream __OutputStream;

    public Stream getOutputStream() {
        return __OutputStream;
    }

    public void setOutputStream(Stream value) {
        __OutputStream = value;
    }

    /**
     * Gets or sets, for sparse transfers, the size of each chunk.
     *
     * A chunk is transfered if any byte in the chunk is valid, otherwise it is
     * not.
     * This value should normally be set to reflect the underlying storage
     * granularity
     * of
     * {@code OutputStream}
     * .
     */
    private int __SparseChunkSize;

    public int getSparseChunkSize() {
        return __SparseChunkSize;
    }

    public void setSparseChunkSize(int value) {
        __SparseChunkSize = value;
    }

    /**
     * Gets or sets a value indicating whether to enable the sparse copy
     * behaviour (default true).
     */
    private boolean __SparseCopy;

    public boolean getSparseCopy() {
        return __SparseCopy;
    }

    public void setSparseCopy(boolean value) {
        __SparseCopy = value;
    }

    /**
     * Event raised periodically through the pump operation.
     *
     * This event is signalled synchronously, so to avoid slowing the pumping
     * activity
     * implementations should return quickly.
     */
    public BiConsumer<Object, PumpProgressEventArgs> ProgressEvent;

    /**
     * Performs the pump activity, blocking until complete.
     */
    public void run() {
        if (getInputStream() == null) {
            throw new UnsupportedOperationException("Input stream is null");
        }

        if (getOutputStream() == null) {
            throw new UnsupportedOperationException("Output stream is null");
        }

        if (!getOutputStream().canSeek()) {
            throw new UnsupportedOperationException("Output stream does not support seek operations");
        }

        if (getSparseChunkSize() <= 1) {
            throw new UnsupportedOperationException("Chunk size is invalid");
        }

        if (getSparseCopy()) {
            runSparse();
        } else {
            runNonSparse();
        }
    }

    private static boolean isAllZeros(byte[] buffer, int offset, int count) {
        for (int j = 0; j < count; j++) {
            if (buffer[offset + j] != 0) {
                return false;
            }

        }
        return true;
    }

    private void runNonSparse() {
        byte[] copyBuffer = new byte[getBufferSize()];
        getInputStream().setPosition(0);
        getOutputStream().setPosition(0);
        int numRead = getInputStream().read(copyBuffer, 0, copyBuffer.length);
        while (numRead > 0) {
            setBytesRead(getBytesRead() + numRead);
            getOutputStream().write(copyBuffer, 0, numRead);
            setBytesWritten(getBytesWritten() + numRead);
            raiseProgressEvent();
            numRead = getInputStream().read(copyBuffer, 0, copyBuffer.length);
        }
    }

    private void runSparse() {
        SparseStream inStream;
        Stream stream =  getInputStream();
        if (!SparseStream.class.isInstance(stream)) {
            inStream = SparseStream.fromStream(stream, Ownership.None);
        } else {
            inStream = SparseStream.class.cast(stream);
        }

        if (getBufferSize() > getSparseChunkSize() && getBufferSize() % getSparseChunkSize() != 0) {
            throw new UnsupportedOperationException("Buffer size is not a multiple of the sparse chunk size");
        }

        byte[] copyBuffer = new byte[Math.max(getBufferSize(), getSparseChunkSize())];
        setBytesRead(0);
        setBytesWritten(0);
        for (StreamExtent extent  : inStream.getExtents()) {
            inStream.setPosition(extent.getStart());
            long extentOffset = 0;
            while (extentOffset < extent.getLength()) {
                int numRead = (int) Math.min(copyBuffer.length, extent.getLength() - extentOffset);
                StreamUtilities.readExact(inStream, copyBuffer, 0, numRead);
                setBytesRead(getBytesRead() + numRead);
                int copyBufferOffset = 0;
                for (int i = 0; i < numRead; i += getSparseChunkSize()) {
                    if (isAllZeros(copyBuffer, i, Math.min(getSparseChunkSize(), numRead - i))) {
                        if (copyBufferOffset < i) {
                            getOutputStream().setPosition(extent.getStart() + extentOffset + copyBufferOffset);
                            getOutputStream().write(copyBuffer, copyBufferOffset, i - copyBufferOffset);
                            setBytesWritten(getBytesWritten() + (i - copyBufferOffset));
                        }

                        copyBufferOffset = i + getSparseChunkSize();
                    }

                }
                if (copyBufferOffset < numRead) {
                    getOutputStream().setPosition(extent.getStart() + extentOffset + copyBufferOffset);
                    getOutputStream().write(copyBuffer, copyBufferOffset, numRead - copyBufferOffset);
                    setBytesWritten(getBytesWritten() + (numRead - copyBufferOffset));
                }

                extentOffset += numRead;
                raiseProgressEvent();
            }
        }
        // Ensure the output stream is at least as long as the input stream.  This uses
        // read/write, rather than SetLength, to avoid failing on streams that can't be
        // explicitly resized.  Side-effect of this, is that if outStream is an NTFS
        // file stream, then actual clusters will be allocated out to at least the
        // length of the input stream.
        if (getOutputStream().getLength() < inStream.getLength()) {
            inStream.setPosition(inStream.getLength() - 1);
            int b = inStream.readByte();
            if (b >= 0) {
                getOutputStream().setPosition(inStream.getLength() - 1);
                getOutputStream().writeByte((byte) b);
            }

        }

    }

    private void raiseProgressEvent() {
        // Raise the event by using the () operator.
        if (ProgressEvent != null) {
            PumpProgressEventArgs args = new PumpProgressEventArgs();
            args.setBytesRead(getBytesRead());
            args.setBytesWritten(getBytesWritten());
            args.setSourcePosition(getInputStream().getPosition());
            args.setDestinationPosition(getOutputStream().getPosition());
            ProgressEvent.accept(this, args);
        }
    }
}
