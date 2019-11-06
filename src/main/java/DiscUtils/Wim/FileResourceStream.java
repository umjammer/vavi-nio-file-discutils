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

package DiscUtils.Wim;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


/**
 * Provides access to a (compressed) resource within the WIM file.
 * Stream access must be strictly sequential.
 */
public class FileResourceStream extends SparseStream {
    private static final int E8DecodeFileSize = 12000000;

    private final Stream _baseStream;

    private final long[] _chunkLength;

    private final long[] _chunkOffsets;

    private final int _chunkSize;

    private int _currentChunk;

    private Stream _currentChunkStream;

    private final ShortResourceHeader _header;

    private final boolean _lzxCompression;

    private final long _offsetDelta;

    private long _position;

    public FileResourceStream(Stream baseStream, ShortResourceHeader header, boolean lzxCompression, int chunkSize) {
        _baseStream = baseStream;
        _header = header;
        _lzxCompression = lzxCompression;
        _chunkSize = chunkSize;
        if (baseStream.getLength() > 0xffff_ffffl) {
            throw new UnsupportedOperationException("Large files >4GB");
        }

        int numChunks = (int) MathUtilities.ceil(header.OriginalSize, _chunkSize);
        _chunkOffsets = new long[numChunks];
        _chunkLength = new long[numChunks];
        for (int i = 1; i < numChunks; ++i) {
            _chunkOffsets[i] = EndianUtilities.toUInt32LittleEndian(StreamUtilities.readExact(_baseStream, 4), 0);
            _chunkLength[i - 1] = _chunkOffsets[i] - _chunkOffsets[i - 1];
        }
        _chunkLength[numChunks - 1] = _baseStream.getLength() - _baseStream.getPosition() - _chunkOffsets[numChunks - 1];
        _offsetDelta = _baseStream.getPosition();
        _currentChunk = -1;
    }

    public boolean canRead() {
        return true;
    }

    public boolean canSeek() {
        return false;
    }

    public boolean canWrite() {
        return false;
    }

    public List<StreamExtent> getExtents() {
        return Arrays.asList(new StreamExtent(0, getLength()));
    }

    public long getLength() {
        return _header.OriginalSize;
    }

    public long getPosition() {
        return _position;
    }

    public void setPosition(long value) {
        _position = value;
    }

    public void flush() {
    }

    public int read(byte[] buffer, int offset, int count) {
        if (_position >= getLength()) {
            return 0;
        }

        int maxToRead = (int) Math.min(getLength() - _position, count);
        int totalRead = 0;
        while (totalRead < maxToRead) {
            int chunk = (int) (_position / _chunkSize);
            int chunkOffset = (int) (_position % _chunkSize);
            int numToRead = Math.min(maxToRead - totalRead, _chunkSize - chunkOffset);
            if (_currentChunk != chunk) {
                _currentChunkStream = openChunkStream(chunk);
                _currentChunk = chunk;
            }

            _currentChunkStream.setPosition(chunkOffset);
            int numRead = _currentChunkStream.read(buffer, offset + totalRead, numToRead);
            if (numRead == 0) {
                return totalRead;
            }

            _position += numRead;
            totalRead += numRead;
        }
        return totalRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        throw new UnsupportedOperationException();
    }

    public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    private Stream openChunkStream(int chunk) {
        int targetUncompressed = _chunkSize;
        if (chunk == _chunkLength.length - 1) {
            targetUncompressed = (int) (getLength() - _position);
        }

        Stream rawChunkStream = new SubStream(_baseStream, _offsetDelta + _chunkOffsets[chunk], _chunkLength[chunk]);
        if ((_header.Flags.ordinal() & ResourceFlags.Compressed.ordinal()) != 0 && _chunkLength[chunk] != targetUncompressed) {
            if (_lzxCompression) {
                return new LzxStream(rawChunkStream, 15, E8DecodeFileSize);
            }

            return new XpressStream(rawChunkStream, targetUncompressed);
        }

        return rawChunkStream;
    }

    @Override
    public void close() throws IOException {
        _currentChunkStream.close();
    }
}
