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

import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * base class for reading binary data from a stream.
 */
public abstract class DataReader {

    private static final int bufferSize = 8;

    protected final Stream stream;

    protected byte[] buffer;

    public DataReader(Stream stream) {
        this.stream = stream;
    }

    public long getLength() {
        return stream.getLength();
    }

    public long getPosition() {
        return stream.getPosition();
    }

    public void skip(int bytes) {
        readBytes(bytes);
    }

    public abstract short readUInt16();

    public abstract int readInt32();

    public abstract int readUInt32();

    public abstract long readInt64();

    public abstract long readUInt64();

    public byte[] readBytes(int count) {
        return StreamUtilities.readExact(stream, count);
    }

    protected void readToBuffer(int count) {
        if (buffer == null) {
            buffer = new byte[bufferSize];
        }

        StreamUtilities.readExact(stream, buffer, 0, count);
    }
}
