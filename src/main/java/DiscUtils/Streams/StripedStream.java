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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class StripedStream extends SparseStream {
    private final boolean _canRead;

    private final boolean _canWrite;

    private final long _length;

    private final Ownership _ownsWrapped;

    private long _position;

    private final long _stripeSize;

    private List<SparseStream> _wrapped;

    public StripedStream(long stripeSize, Ownership ownsWrapped, List<SparseStream> wrapped) {
        _wrapped = new ArrayList<>(wrapped);
        _stripeSize = stripeSize;
        _ownsWrapped = ownsWrapped;
        _canRead = _wrapped.get(0).canRead();
        _canWrite = _wrapped.get(0).canWrite();
        long subStreamLength = _wrapped.get(0).getLength();
        for (SparseStream stream : _wrapped) {
            if (stream.canRead() != _canRead || stream.canWrite() != _canWrite) {
                throw new IllegalArgumentException("All striped streams must have the same read/write permissions");
            }

            if (stream.getLength() != subStreamLength) {
                throw new IllegalArgumentException("All striped streams must have the same length");
            }

        }
        _length = subStreamLength * wrapped.size();
    }

    public boolean canRead() {
        return _canRead;
    }

    public boolean canSeek() {
        return true;
    }

    public boolean canWrite() {
        return _canWrite;
    }

    public List<StreamExtent> getExtents() {
        return Arrays.asList(new StreamExtent(0, _length));
    }

    // Temporary, indicate there are no 'unstored' extents.
    // Consider combining extent information from all wrapped streams in future.
    public long getLength() {
        return _length;
    }

    public long getPosition() {
        return _position;
    }

    public void setPosition(long value) {
        _position = value;
    }

    public void flush() {
        for (SparseStream stream : _wrapped) {
            stream.flush();
        }
    }

    public int read(byte[] buffer, int offset, int count) {
        if (!canRead()) {
            throw new UnsupportedOperationException("Attempt to read to non-readable stream");
        }

        int maxToRead = (int) Math.min(_length - _position, count);
        int totalRead = 0;
        while (totalRead < maxToRead) {
            long stripe = _position / _stripeSize;
            long stripeOffset = _position % _stripeSize;
            int stripeToRead = (int) Math.min(maxToRead - totalRead, _stripeSize - stripeOffset);
            int streamIdx = (int) (stripe % _wrapped.size());
            long streamStripe = stripe / _wrapped.size();
            Stream targetStream = _wrapped.get(streamIdx);
            targetStream.setPosition(streamStripe * _stripeSize + stripeOffset);
            int numRead = targetStream.read(buffer, offset + totalRead, stripeToRead);
            _position += numRead;
            totalRead += numRead;
        }
        return totalRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += _position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += _length;
        }

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of stream");
        }

        _position = effectiveOffset;
        return _position;
    }

    public void setLength(long value) {
        if (value != _length) {
            throw new UnsupportedOperationException("Changing the stream length is not permitted for striped streams");
        }

    }

    public void write(byte[] buffer, int offset, int count) {
        if (!canWrite()) {
            throw new dotnet4j.io.IOException("Attempt to write to read-only stream");
        }

        if (_position + count > _length) {
            throw new dotnet4j.io.IOException("Attempt to write beyond end of stream");
        }

        int totalWritten = 0;
        while (totalWritten < count) {
            long stripe = _position / _stripeSize;
            long stripeOffset = _position % _stripeSize;
            int stripeToWrite = (int) Math.min(count - totalWritten, _stripeSize - stripeOffset);
            int streamIdx = (int) (stripe % _wrapped.size());
            long streamStripe = stripe / _wrapped.size();
            Stream targetStream = _wrapped.get(streamIdx);
            targetStream.setPosition(streamStripe * _stripeSize + stripeOffset);
            targetStream.write(buffer, offset + totalWritten, stripeToWrite);
            _position += stripeToWrite;
            totalWritten += stripeToWrite;
        }
    }

    public void close() throws IOException {
        if (_ownsWrapped == Ownership.Dispose && _wrapped != null) {
            for (SparseStream stream : _wrapped) {
                stream.close();
            }
            _wrapped = null;
        }
    }
}
