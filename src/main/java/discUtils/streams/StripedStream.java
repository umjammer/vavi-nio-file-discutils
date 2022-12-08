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


public class StripedStream extends SparseStream {

    private final boolean canRead;

    private final boolean canWrite;

    private final long length;

    private final Ownership ownsWrapped;

    private long position;

    private final long stripeSize;

    private List<SparseStream> wrapped;

    public StripedStream(long stripeSize, Ownership ownsWrapped, List<SparseStream> wrapped) {
        this.wrapped = new ArrayList<>(wrapped);
        this.stripeSize = stripeSize;
        this.ownsWrapped = ownsWrapped;
        canRead = this.wrapped.get(0).canRead();
        canWrite = this.wrapped.get(0).canWrite();
        long subStreamLength = this.wrapped.get(0).getLength();
        for (SparseStream stream : this.wrapped) {
            if (stream.canRead() != canRead || stream.canWrite() != canWrite) {
                throw new IllegalArgumentException("All striped streams must have the same read/write permissions");
            }

            if (stream.getLength() != subStreamLength) {
                throw new IllegalArgumentException("All striped streams must have the same length");
            }

        }
        length = subStreamLength * wrapped.size();
    }

    @Override public boolean canRead() {
        return canRead;
    }

    @Override public boolean canSeek() {
        return true;
    }

    @Override public boolean canWrite() {
        return canWrite;
    }

    @Override public List<StreamExtent> getExtents() {
        return Collections.singletonList(new StreamExtent(0, length));
    }

    // Temporary, indicate there are no 'unstored' extents.
    // Consider combining extent information from all wrapped streams in future.
    @Override public long getLength() {
        return length;
    }

    @Override public long position() {
        return position;
    }

    @Override public void position(long value) {
        position = value;
    }

    @Override public void flush() {
        for (SparseStream stream : wrapped) {
            stream.flush();
        }
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        if (!canRead()) {
            throw new UnsupportedOperationException("Attempt to read to non-readable stream");
        }

        int maxToRead = (int) Math.min(length - position, count);
        int totalRead = 0;
        while (totalRead < maxToRead) {
            long stripe = position / stripeSize;
            long stripeOffset = position % stripeSize;
            int stripeToRead = (int) Math.min(maxToRead - totalRead, stripeSize - stripeOffset);
            int streamIdx = (int) (stripe % wrapped.size());
            long streamStripe = stripe / wrapped.size();
            Stream targetStream = wrapped.get(streamIdx);
            targetStream.position(streamStripe * stripeSize + stripeOffset);
            int numRead = targetStream.read(buffer, offset + totalRead, stripeToRead);
            position += numRead;
            totalRead += numRead;
        }
        return totalRead;
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += length;
        }

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of stream");
        }

        position = effectiveOffset;
        return position;
    }

    @Override public void setLength(long value) {
        if (value != length) {
            throw new UnsupportedOperationException("Changing the stream length is not permitted for striped streams");
        }
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        if (!canWrite()) {
            throw new dotnet4j.io.IOException("Attempt to write to read-only stream");
        }

        if (position + count > length) {
            throw new dotnet4j.io.IOException("Attempt to write beyond end of stream");
        }

        int totalWritten = 0;
        while (totalWritten < count) {
            long stripe = position / stripeSize;
            long stripeOffset = position % stripeSize;
            int stripeToWrite = (int) Math.min(count - totalWritten, stripeSize - stripeOffset);
            int streamIdx = (int) (stripe % wrapped.size());
            long streamStripe = stripe / wrapped.size();
            Stream targetStream = wrapped.get(streamIdx);
            targetStream.position(streamStripe * stripeSize + stripeOffset);
            targetStream.write(buffer, offset + totalWritten, stripeToWrite);
            position += stripeToWrite;
            totalWritten += stripeToWrite;
        }
    }

    @Override public void close() throws IOException {
        if (ownsWrapped == Ownership.Dispose && wrapped != null) {
            for (SparseStream stream : wrapped) {
                stream.close();
            }
            wrapped = null;
        }
    }
}
