//
// Copyright (c) 2017, Bianco Veigel
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

import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import moe.yo3explorer.dotnetio4j.SeekOrigin;


/**
 * Stream wrapper to allow forward only seeking on not seekable streams
 */
public class PositionWrappingStream extends WrappingStream {
    public PositionWrappingStream(SparseStream toWrap, long currentPosition, Ownership ownership) {
        super(toWrap, ownership);
        _position = currentPosition;
    }

    private long _position;

    public long getPosition() {
        return _position;
    }

    public void setPosition(long value) {
        if (_position == value)
            return;

        seek(value, SeekOrigin.Begin);
    }

    public long seek(long offset, SeekOrigin origin) {
        if (super.canSeek()) {
            return super.seek(offset, SeekOrigin.Current);
        }

        switch (origin) {
        case Begin:
            offset = offset - _position;
            break;
        case Current:
            offset = offset + _position;
            break;
        case End:
            offset = getLength() - offset;
            break;
        }
        if (offset == 0)
            return _position;

        if (offset < 0)
            throw new UnsupportedOperationException("backward seeking is not supported");

        byte[] buffer = new byte[(int) Sizes.OneKiB];
        while (offset > 0) {
            int read = super.read(buffer, 0, (int) Math.min(buffer.length, offset));
            offset -= read;
        }
        return _position;
    }

    public boolean canSeek() {
        return true;
    }

    public int read(byte[] buffer, int offset, int count) {
        int read = super.read(buffer, offset, count);
        _position += read;
        return read;
    }

    public void write(byte[] buffer, int offset, int count) {
        super.write(buffer, offset, count);
        _position += count;
    }

}
