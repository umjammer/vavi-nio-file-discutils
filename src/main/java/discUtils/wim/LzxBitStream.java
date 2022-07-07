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

import discUtils.core.compression.BitStream;
import dotnet4j.io.Stream;


/**
 * Converts a byte stream into a bit stream.
 * To avoid alignment issues, the bit stream is infinitely long. Once the
 * converted byte stream is consumed, an infinite sequence of zero's is
 * emulated.It is strongly recommended to use some kind of in memory buffering
 * (such as a
 * BufferedStream) for the wrapped stream. This class makes a large number of
 * small
 * reads..
 */
public final class LzxBitStream extends BitStream {
    private int _buffer;

    private int _bufferAvailable;

    private final Stream _byteStream;

    private long _position;

    private final byte[] _readBuffer = new byte[2];

    public LzxBitStream(Stream byteStream) {
        _byteStream = byteStream;
    }

    public int getMaxReadAhead() {
        return 16;
    }

    public int read(int count) {
        if (count > 16) {
            throw new IndexOutOfBoundsException("Maximum 32 bits can be read");
        }

        if (_bufferAvailable < count) {
            need(count);
        }

        _bufferAvailable -= count;
        _position += count;
        int mask = (1 << count) - 1;
        return (_buffer >>> _bufferAvailable) & mask;
    }

    public int peek(int count) {
        if (_bufferAvailable < count) {
            need(count);
        }

        int mask = (1 << count) - 1;
        return (_buffer >>> (_bufferAvailable - count)) & mask;
    }

    public void consume(int count) {
        if (_bufferAvailable < count) {
            need(count);
        }

        _bufferAvailable -= count;
        _position += count;
    }

    public void align(int bits) {
        // Note: Consumes 1-16 bits, to force alignment (never 0)
        int offset = (int) (_position % bits);
        consume(bits - offset);
    }

    public int readBytes(byte[] buffer, int offset, int count) {
        if (_position % 8 != 0) {
            throw new UnsupportedOperationException("Attempt to read bytes when not byte-aligned");
        }

        int totalRead = 0;
        while (totalRead < count) {
            int numRead = _byteStream.read(buffer, offset + totalRead, count - totalRead);
            if (numRead == 0) {
                _position += totalRead * 8L;
                return totalRead;
            }

            totalRead += numRead;
        }
        _position += totalRead * 8L;
        return totalRead;
    }

    public byte[] readBytes(int count) {
        if (_position % 8 != 0) {
            throw new UnsupportedOperationException("Attempt to read bytes when not byte-aligned");
        }

        byte[] buffer = new byte[count];
        readBytes(buffer, 0, count);
        return buffer;
    }

    private void need(int count) {
        while (_bufferAvailable < count) {
            _readBuffer[0] = 0;
            _readBuffer[1] = 0;
            _byteStream.read(_readBuffer, 0, 2);
            _buffer = _buffer << 16 | (_readBuffer[1] & 0xff) << 8 | (_readBuffer[0] & 0xff);
            _bufferAvailable += 16;
        }
    }
}
