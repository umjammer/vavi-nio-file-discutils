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
import java.util.Arrays;
import java.util.List;

import discUtils.streams.util.Ownership;
import dotnet4j.io.SeekOrigin;


/**
 * The concatenation of multiple streams (read-only, for now).
 */
public class ConcatStream extends SparseStream {

    private boolean canWrite;

    private final Ownership ownsStreams;

    private long position;

    private List<SparseStream> streams;

    public ConcatStream(Ownership ownsStreams, SparseStream... streams) {
        this(ownsStreams, Arrays.asList(streams));
    }

    public ConcatStream(Ownership ownsStreams, List<SparseStream> streams) {
        this.ownsStreams = ownsStreams;
        this.streams = streams;

        // Only allow writes if all streams can be written
        canWrite = true;
        for (SparseStream stream : streams) {
            if (!stream.canWrite()) {
                canWrite = false;
            }
        }
    }

    @Override public boolean canRead() {
        checkDisposed();
        return true;
    }

    @Override public boolean canSeek() {
        checkDisposed();
        return true;
    }

    @Override public boolean canWrite() {
        checkDisposed();
        return canWrite;
    }

    @Override public List<StreamExtent> getExtents() {
        checkDisposed();
        List<StreamExtent> extents = new ArrayList<>();

        long pos = 0;
        for (SparseStream stream : streams) {
            for (StreamExtent extent : stream.getExtents()) {
                extents.add(new StreamExtent(extent.getStart() + pos, extent.getLength()));
            }

            pos += stream.getLength();
        }
        return extents;
    }

    @Override public long getLength() {
        checkDisposed();
        long length = 0;

        for (SparseStream stream : streams) {
            length += stream.getLength();
        }

        return length;
    }

    @Override public long position() {
        checkDisposed();
        return position;
    }

    @Override public void position(long value) {
        checkDisposed();
        position = value;
    }

    @Override public void flush() {
        checkDisposed();
        for (SparseStream stream : streams) {
            stream.flush();
        }
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        checkDisposed();

        int totalRead = 0;
        int numRead;

        do {
            long[] activeStreamStartPos = new long[1];
            int activeStream = getActiveStream(activeStreamStartPos);

            streams.get(activeStream).position(position - activeStreamStartPos[0]);

            numRead = streams.get(activeStream).read(buffer, offset + totalRead, count - totalRead);

            totalRead += numRead;
            position += numRead;
        } while (numRead != 0);

        return totalRead;
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        checkDisposed();

        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += getLength();
        }

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
        }
        position(effectiveOffset);
        return position();
    }

    @Override public void setLength(long value) {
        checkDisposed();

        long[] lastStreamOffset = new long[1];
        int lastStream = getStream(getLength(), lastStreamOffset);
        if (value < lastStreamOffset[0]) {
            throw new dotnet4j.io.IOException("Unable to reduce stream length to less than %d".formatted(lastStreamOffset));
        }

        streams.get(lastStream).setLength(value - lastStreamOffset[0]);
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        checkDisposed();

        int totalWritten = 0;
        while (totalWritten != count) {
            // Offset of the stream = streamOffset
            long[] streamOffset = new long[1];
            int streamIdx = getActiveStream(streamOffset);

            // Offset within the stream = streamPos
            long streamPos = position - streamOffset[0];
            streams.get(streamIdx).position(streamPos);

            // Write (limited to the stream's length), except for final stream - that may be
            // extendable
            int numToWrite;
            if (streamIdx == streams.size() - 1) {
                numToWrite = count - totalWritten;
            } else {
                numToWrite = (int) Math.min(count - totalWritten, streams.get(streamIdx).getLength() - streamPos);
            }

            streams.get(streamIdx).write(buffer, offset + totalWritten, numToWrite);

            totalWritten += numToWrite;
            position += numToWrite;
        }
    }

    @Override public void close() throws IOException {
        if (ownsStreams == Ownership.Dispose && streams != null) {
            for (SparseStream stream : streams) {
                stream.close();
            }
            streams = null;
        }
    }

    /**
     * @param startPos {@cs out}
     */
    private int getActiveStream(long[] startPos) {
        return getStream(position, startPos);
    }

    /**
     * @param streamStartPos {@cs out}
     */
    private int getStream(long targetPos, long[] streamStartPos) {
        // Find the stream that position is within
        streamStartPos[0] = 0;
        int focusStream = 0;
        while (focusStream < streams.size() - 1 && streamStartPos[0] + streams.get(focusStream).getLength() <= targetPos) {
            streamStartPos[0] = streamStartPos[0] + streams.get(focusStream).getLength();
            focusStream++;
        }

        return focusStream;
    }

    private void checkDisposed() {
        if (streams == null) {
            throw new dotnet4j.io.IOException("it has been closed.");
        }
    }
}
