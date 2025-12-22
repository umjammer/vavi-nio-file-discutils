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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import discUtils.streams.util.Ownership;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class SubStream extends MappedStream {

    private static final Logger logger = System.getLogger(SubStream.class.getName());

    private final long first;

    private final long length;

    private final Ownership ownsParent;

    private final Stream parent;

    private long position;

    public SubStream(Stream parent, long first, long length) {
        this.parent = parent;
        this.first = first;
        this.length = length;
        ownsParent = Ownership.None;
logger.log(Level.TRACE, "sub: first: %d, length: %d".formatted(first, length));

        if (this.first + this.length > this.parent.getLength()) {
            throw new IllegalArgumentException("Substream extends beyond end of parent stream");
        }
    }

    public SubStream(Stream parent, Ownership ownsParent, long first, long length) {
        this.parent = parent;
        this.ownsParent = ownsParent;
        this.first = first;
        this.length = length;

        if (this.first + this.length > this.parent.getLength()) {
            throw new IllegalArgumentException("Substream extends beyond end of parent stream");
        }
    }

    @Override
    public boolean canRead() {
        return parent.canRead();
    }

    @Override
    public boolean canSeek() {
        return parent.canSeek();
    }

    @Override
    public boolean canWrite() {
        return parent.canWrite();
    }

    @Override
    public List<StreamExtent> getExtents() {
        if (parent instanceof SparseStream) {
            return offsetExtents(((SparseStream) parent).getExtentsInRange(first, length));
        }
        return Collections.singletonList(new StreamExtent(0, length));
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public void position(long value) {
        if (value <= length) {
            position = value;
        } else {
            throw new IllegalArgumentException("value: Attempt to move beyond end of stream");
        }
    }

    @Override
    public List<StreamExtent> mapContent(long start, long length) {
        return Collections.singletonList(new StreamExtent(start + first, length));
    }

    @Override
    public void flush() {
        parent.flush();
    }

    @Override
    public int read(byte[] buffer, int offset, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count: Attempt to read negative bytes");
        }

        if (position > length) {
            return 0;
        }

logger.log(Level.TRACE, "sub: position: " + (first + position) + ", " + first + ", " + position);
        parent.position(first + position);
        int numRead = parent.read(buffer, offset, (int) Math.min(count, Math.min(length - position, Integer.MAX_VALUE)));
//if (numRead > 1) logger.log(Level.DEBUG, parent + ", " + first + ", " + position + ", " + numRead + "\n" + StringUtil.getDump(buffer, offset, Math.min(64, numRead)));
        position += numRead;
        return numRead;
    }

    @Override
    public long seek(long offset, SeekOrigin origin) {
        long absNewPos = offset;
        if (origin == SeekOrigin.Current) {
            absNewPos += position;
        } else if (origin == SeekOrigin.End) {
            absNewPos += length;
        }

        if (absNewPos < 0) {
            throw new IllegalArgumentException("offset: Attempt to move before start of stream");
        }

        position = absNewPos;
        return position;
    }

    @Override
    public void setLength(long value) {
        throw new UnsupportedOperationException("Attempt to change length of a substream");
    }

    @Override
    public void write(byte[] buffer, int offset, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count: Attempt to write negative bytes");
        }

        if (position + count > length) {
            throw new IllegalArgumentException("count: Attempt to write beyond end of substream");
        }

        parent.position(first + position);
        parent.write(buffer, offset, count);
        position += count;
    }

    @Override
    public void close() throws IOException {
        if (ownsParent == Ownership.Dispose) {
            parent.close();
        }
    }

    private List<StreamExtent> offsetExtents(List<StreamExtent> src) {
        return src.stream().map(e -> new StreamExtent(e.getStart() - first, e.getLength())).collect(Collectors.toList());
    }
}
