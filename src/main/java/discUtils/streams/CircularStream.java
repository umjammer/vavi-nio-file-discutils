//
// Copyright (c) 2008-2013, Kenneth Bell
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

import discUtils.streams.util.Ownership;


/**
 * Represents a stream that is circular, so reads and writes off the end of the
 * stream wrap.
 */
public final class CircularStream extends WrappingStream {
    public CircularStream(SparseStream toWrap, Ownership ownership) {
        super(toWrap, ownership);
    }

    public int read(byte[] buffer, int offset, int count) {
        wrapPosition();
        int read = super.read(buffer, offset, (int) Math.min(getLength() - getPosition(), count));
        wrapPosition();
        return read;
    }

    public void write(byte[] buffer, int offset, int count) {
        wrapPosition();
        int totalWritten = 0;
        while (totalWritten < count) {
            int toWrite = (int) Math.min(count - totalWritten, getLength() - getPosition());
            super.write(buffer, offset + totalWritten, toWrite);
            wrapPosition();
            totalWritten += toWrite;
        }
    }

    private void wrapPosition() {
        long pos = getPosition();
        long length = getLength();
        if (pos >= length) {
            setPosition(pos % length);
        }
    }
}
