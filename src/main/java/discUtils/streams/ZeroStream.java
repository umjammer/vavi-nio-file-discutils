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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dotnet4j.io.SeekOrigin;


/**
 * A stream that returns Zero's.
 */
public class ZeroStream extends MappedStream {

    private boolean atEof;

    private final long length;

    private long position;

    public ZeroStream(long length) {
        this.length = length;
    }

    @Override public boolean canRead() {
        return true;
    }

    @Override public boolean canSeek() {
        return true;
    }

    @Override public boolean canWrite() {
        return false;
    }

    @Override public List<StreamExtent> getExtents() {
        // The stream is entirely sparse
        return Collections.emptyList();
    }

    @Override public long getLength() {
        return length;
    }

    @Override public long position() {
        return position;
    }

    @Override public void position(long value) {
        position = value;
        atEof = false;
    }

    @Override public List<StreamExtent> mapContent(long start, long length) {
        return Collections.emptyList();
    }

    @Override public void flush() {
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        if (position > length) {
            atEof = true;
            throw new dotnet4j.io.IOException("Attempt to read beyond end of stream");
        }

        if (position == length) {
            if (atEof) {
                throw new dotnet4j.io.IOException("Attempt to read beyond end of stream");
            }

            atEof = true;
            return 0;
        }

        int numToClear = (int) Math.min(count, length - position);
        Arrays.fill(buffer, offset, offset + numToClear, (byte) 0);
        position += numToClear;

        return numToClear;
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += length;
        }

        atEof = false;

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of stream");
        }
        position = effectiveOffset;
        return position;
    }

    @Override public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
    }
}
