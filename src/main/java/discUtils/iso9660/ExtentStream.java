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

package discUtils.iso9660;

import java.io.IOException;

import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class ExtentStream extends Stream {

    private final long dataLength;

    private final byte fileUnitSize;

    private final byte interleaveGapSize;

    private final Stream isoStream;

    private long position;

    private final int startBlock;

    public ExtentStream(Stream isoStream, int startBlock, long dataLength, byte fileUnitSize, byte interleaveGapSize) {
        this.isoStream = isoStream;
        this.startBlock = startBlock;
        this.dataLength = dataLength;
        this.fileUnitSize = fileUnitSize;
        this.interleaveGapSize = interleaveGapSize;
        if (this.fileUnitSize != 0 || this.interleaveGapSize != 0) {
            throw new UnsupportedOperationException("Non-contiguous extents not supported");
        }
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

    @Override public long getLength() {
        return dataLength;
    }

    @Override public long position() {
        return position;
    }

    @Override public void position(long value) {
        position = value;
    }

    @Override public void flush() {
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        if (position > dataLength) {
            return 0;
        }

        int toRead = (int) Math.min(count, dataLength - position);
        isoStream.position(position + startBlock * (long) IsoUtilities.SectorSize);
        int numRead = isoStream.read(buffer, offset, toRead);
        position += numRead;
        return numRead;
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        long newPos = offset;
        if (origin == SeekOrigin.Current) {
            newPos += position;
        } else if (origin == SeekOrigin.End) {
            newPos += dataLength;
        }

        position = newPos;
        return newPos;
    }

    @Override public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        isoStream.close();
    }
}
