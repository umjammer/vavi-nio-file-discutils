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

package discUtils.streams.readerWriter;

import dotnet4j.io.Stream;

public abstract class DataWriter {

    private static final int bufferSize = 8; // sizeof(UInt64)

    protected final Stream stream;

    protected byte[] buffer;

    public DataWriter(Stream stream) {
        this.stream = stream;
    }

    public abstract void write(short value);

    public abstract void write(int value);

    public abstract void write(long value);

    public void writeBytes(byte[] value, int offset, int count) {
        stream.write(value, offset, count);
    }

    public void writeBytes(byte[] value) {
        stream.write(value, 0, value.length);
    }

    public void flush() {
        stream.flush();
    }

    protected void ensureBuffer() {
        if (buffer == null) {
            buffer = new byte[bufferSize];
        }
    }

    protected void flushBuffer(int count) {
        stream.write(buffer, 0, count);
    }
}
