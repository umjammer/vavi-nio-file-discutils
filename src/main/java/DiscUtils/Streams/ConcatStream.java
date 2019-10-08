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
import java.util.List;

import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.SeekOrigin;


/**
 * The concatenation of multiple streams (read-only, for now).
 */
public class ConcatStream extends SparseStream {
    private boolean _canWrite;

    private final Ownership _ownsStreams;

    private long _position;

    private List<SparseStream> _streams;

    public ConcatStream(Ownership ownsStreams, List<SparseStream> streams) {
        _ownsStreams = ownsStreams;
        _streams = streams;
        // Only allow writes if all streams can be written
        _canWrite = true;
        for (SparseStream stream : streams) {
            if (!stream.canWrite()) {
                _canWrite = false;
            }
        }
    }

    public boolean canRead() {
        checkDisposed();
        return true;
    }

    public boolean canSeek() {
        checkDisposed();
        return true;
    }

    public boolean canWrite() {
        checkDisposed();
        return _canWrite;
    }

    public List<StreamExtent> getExtents() {
        checkDisposed();
        List<StreamExtent> extents = new ArrayList<>();
        long pos = 0;
        for (int i = 0; i < _streams.size(); ++i) {
            for (StreamExtent extent : _streams.get(i).getExtents()) {
                extents.add(new StreamExtent(extent.getStart() + pos, extent.getLength()));
            }
            pos += _streams.get(i).getLength();
        }
        return extents;
    }

    public long getLength() {
        checkDisposed();
        long length = 0;
        for (int i = 0; i < _streams.size(); ++i) {
            length += _streams.get(i).getLength();
        }
        return length;
    }

    public long getPosition() {
        checkDisposed();
        return _position;
    }

    public void setPosition(long value) {
        checkDisposed();
        _position = value;
    }

    public void flush() {
        checkDisposed();
        for (int i = 0; i < _streams.size(); ++i) {
//            _streams[i].flush();
        }
    }

    public int read(byte[] buffer, int offset, int count) {
        checkDisposed();
        int totalRead = 0;
        int numRead = 0;
        do {
            long[] activeStreamStartPos = new long[1];
            int activeStream = getActiveStream(activeStreamStartPos);
            _streams.get(activeStream).setPosition(_position - activeStreamStartPos[0]);
            numRead = _streams.get(activeStream).read(buffer, offset + totalRead, count - totalRead);
            totalRead += numRead;
            _position += numRead;
        } while (numRead != 0);
        return totalRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        checkDisposed();
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += _position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += getLength();
        }

        if (effectiveOffset < 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to move before beginning of disk");
        }

        setPosition(effectiveOffset);
        return getPosition();
    }

    public void setLength(long value) {
        checkDisposed();
        long[] lastStreamOffset = new long[1];
        int lastStream = getStream(getLength(), lastStreamOffset);
        if (value < lastStreamOffset[0]) {
            throw new moe.yo3explorer.dotnetio4j.IOException(String.format("Unable to reduce stream length to less than %d", lastStreamOffset));
        }

        _streams.get(lastStream).setLength(value - lastStreamOffset[0]);
    }

    public void write(byte[] buffer, int offset, int count) {
        checkDisposed();
        int totalWritten = 0;
        while (totalWritten != count) {
            // Offset of the stream = streamOffset
            long[] streamOffset = new long[1];
            int streamIdx = getActiveStream(streamOffset);
            // Offset within the stream = streamPos
            long streamPos = _position - streamOffset[0];
            _streams.get(streamIdx).setPosition(streamPos);
            // Write (limited to the stream's length), except for final stream - that may be
            // extendable
            int numToWrite;
            if (streamIdx == _streams.size() - 1) {
                numToWrite = count - totalWritten;
            } else {
                numToWrite = (int) Math.min(count - totalWritten, _streams.get(streamIdx).getLength() - streamPos);
            }
            _streams.get(streamIdx).write(buffer, offset + totalWritten, numToWrite);
            totalWritten += numToWrite;
            _position += numToWrite;
        }
    }

    public void close() throws IOException {
        if (_ownsStreams == Ownership.Dispose && _streams != null) {
            for (SparseStream stream : _streams) {
                stream.close();
            }
            _streams = null;
        }
    }

    private int getActiveStream(long[] startPos) {
        startPos[0] = getStream(_position, startPos);
        return (int) startPos[0];
    }

    private int getStream(long targetPos, long[] streamStartPos) {
        // Find the stream that _position is within
        streamStartPos[0] = 0;
        int focusStream = 0;
        while (focusStream < _streams.size() - 1 && streamStartPos[0] + _streams.get(focusStream).getLength() <= targetPos) {
            streamStartPos[0] = streamStartPos[0] + _streams.get(focusStream).getLength();
            focusStream++;
        }
        return focusStream;
    }

    private void checkDisposed() {
        if (_streams == null) {
            throw new moe.yo3explorer.dotnetio4j.IOException("ConcatStream");
        }
    }
}
