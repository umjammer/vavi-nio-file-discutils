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

import DiscUtils.Core.Compression.BitStream;
import dotnet4j.io.Stream;


/**
 * Converts a byte stream into a bit stream.
 * Note the precise read-ahead behaviour of this stream is critical.
 * Some data is read directly from the underlying stream when decoding an Xpress
 * stream - so it's critical the underlying stream position is in the correct
 * location.
 */
public class XpressBitStream extends BitStream {
    private int _buffer;

    private int _bufferAvailable;

    private final Stream _byteStream;

    private final byte[] _readBuffer = new byte[2];

    public XpressBitStream(Stream byteStream) {
        _byteStream = byteStream;
    }

    public int getMaxReadAhead() {
        return 16;
    }

    public int read(int count) {
        if (count > 16) {
            throw new IndexOutOfBoundsException("Maximum 16 bits can be read");
        }

        ensureBufferFilled();
        _bufferAvailable -= count;
        int mask = (1 << count) - 1;
        return (_buffer >>> _bufferAvailable) & mask;
    }

    public int peek(int count) {
        ensureBufferFilled();
        int mask = (1 << count) - 1;
        return (_buffer >>> (_bufferAvailable - count)) & mask;
    }

    public void consume(int count) {
        ensureBufferFilled();
        _bufferAvailable -= count;
    }

    private void ensureBufferFilled() {
        if (_bufferAvailable < 16) {
            _readBuffer[0] = 0;
            _readBuffer[1] = 0;
            _byteStream.read(_readBuffer, 0, 2);
            _buffer = _buffer << 16 | (_readBuffer[1] & 0xff) << 8 | (_readBuffer[0] & 0xff);
            _bufferAvailable += 16;
        }
    }
}
