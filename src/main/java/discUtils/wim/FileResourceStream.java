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

package discUtils.wim;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.SubStream;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


/**
 * Provides access to a (compressed) resource within the WIM file.
 * Stream access must be strictly sequential.
 */
public class FileResourceStream extends SparseStream {

    private static final int E8DecodeFileSize = 12000000;

    private final Stream baseStream;

    private final long[] chunkLength;

    private final long[] chunkOffsets;

    private final int chunkSize;

    private int currentChunk;

    private Stream currentChunkStream;

    private final ShortResourceHeader header;

    private final boolean lzxCompression;

    private final long offsetDelta;

    private long position;

    public FileResourceStream(Stream baseStream, ShortResourceHeader header, boolean lzxCompression, int chunkSize) {
        this.baseStream = baseStream;
        this.header = header;
        this.lzxCompression = lzxCompression;
        this.chunkSize = chunkSize;
        if (baseStream.getLength() > 0xffff_ffffL) {
            throw new UnsupportedOperationException("Large files >4GB");
        }

        int numChunks = (int) MathUtilities.ceil(header.originalSize, this.chunkSize);
        chunkOffsets = new long[numChunks];
        chunkLength = new long[numChunks];
        for (int i = 1; i < numChunks; ++i) {
            chunkOffsets[i] = EndianUtilities.toUInt32LittleEndian(StreamUtilities.readExact(this.baseStream, 4), 0);
            chunkLength[i - 1] = chunkOffsets[i] - chunkOffsets[i - 1];
        }
        chunkLength[numChunks - 1] = this.baseStream.getLength() - this.baseStream.getPosition() - chunkOffsets[numChunks - 1];
        offsetDelta = this.baseStream.getPosition();
        currentChunk = -1;
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
        return Collections.singletonList(new StreamExtent(0, getLength()));
    }

    public long getLength() {
        return header.originalSize;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long value) {
        position = value;
    }

    public void flush() {
    }

    public int read(byte[] buffer, int offset, int count) {
        if (position >= getLength()) {
            return 0;
        }

        int maxToRead = (int) Math.min(getLength() - position, count);
        int totalRead = 0;
        while (totalRead < maxToRead) {
            int chunk = (int) (position / chunkSize);
            int chunkOffset = (int) (position % chunkSize);
            int numToRead = Math.min(maxToRead - totalRead, chunkSize - chunkOffset);
            if (currentChunk != chunk) {
                currentChunkStream = openChunkStream(chunk);
                currentChunk = chunk;
            }

            currentChunkStream.setPosition(chunkOffset);
            int numRead = currentChunkStream.read(buffer, offset + totalRead, numToRead);
            if (numRead == 0) {
                return totalRead;
            }

            position += numRead;
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
        int targetUncompressed = chunkSize;
        if (chunk == chunkLength.length - 1) {
            targetUncompressed = (int) (getLength() - position);
        }

        Stream rawChunkStream = new SubStream(baseStream, offsetDelta + chunkOffsets[chunk], chunkLength[chunk]);
        if (header.flags.contains(ResourceFlags.Compressed) && chunkLength[chunk] != targetUncompressed) {
            if (lzxCompression) {
                return new LzxStream(rawChunkStream, 15, E8DecodeFileSize);
            }

            return new XpressStream(rawChunkStream, targetUncompressed);
        }

        return rawChunkStream;
    }

    @Override
    public void close() throws IOException {
        currentChunkStream.close();
    }
}
