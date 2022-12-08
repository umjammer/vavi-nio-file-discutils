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
//
// Based on "libbzip2", Copyright (C) 1996-2007 Julian R Seward.
//

package discUtils.core.compression;

import java.io.IOException;

import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class BZip2RleStream extends Stream {

    private byte[] blockBuffer;

    private int blockOffset;

    private int blockRemaining;

    private byte lastByte;

    private int numSame;

    private long position;

    private int runBytesOutstanding;

    public boolean getAtEof() {
        return runBytesOutstanding == 0 && blockRemaining == 0;
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

    public long getLength() {
        throw new UnsupportedOperationException();
    }

    @Override public long position() {
        return position;
    }

    @Override public void position(long value) {
        throw new UnsupportedOperationException();
    }

    public void reset(byte[] buffer, int offset, int count) {
        position = 0;
        blockBuffer = buffer;
        blockOffset = offset;
        blockRemaining = count;
        numSame = -1;
        lastByte = 0;
        runBytesOutstanding = 0;
    }

    public void flush() {
        throw new UnsupportedOperationException();
    }

    public int read(byte[] buffer, int offset, int count) {
        int numRead = 0;
        while (numRead < count && runBytesOutstanding > 0) {
            int runCount = Math.min(runBytesOutstanding, count);
            for (int i = 0; i < runCount; ++i) {
                buffer[offset + numRead] = lastByte;
            }

            runBytesOutstanding -= runCount;
            numRead += runCount;
        }
        while (numRead < count && blockRemaining > 0) {
            byte b = blockBuffer[blockOffset];
            ++blockOffset;
            --blockRemaining;

            if (numSame == 4) {
                int runCount = Math.min(b, count - numRead);
                for (int i = 0; i < runCount; ++i) {
                    buffer[offset + numRead] = lastByte;
                    numRead++;
                }

                runBytesOutstanding = b - runCount;
                numSame = 0;
            } else {
                if (b != lastByte || numSame <= 0) {
                    lastByte = b;
                    numSame = 0;
                }

                buffer[offset + numRead] = b;
                numRead++;
                numSame++;
            }
        }

        position += numRead;
        return numRead;
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

    @Override
    public void close() throws IOException {
    }
}
