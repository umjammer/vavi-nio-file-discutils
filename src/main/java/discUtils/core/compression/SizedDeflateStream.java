//
// Copyright (c) 2014, Quamotion
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

package discUtils.core.compression;

import dotnet4j.io.Stream;
import dotnet4j.io.compression.CompressionMode;
import dotnet4j.io.compression.DeflateStream;


public class SizedDeflateStream extends DeflateStream {

    private final int length;

    private int position;

    public SizedDeflateStream(Stream stream, CompressionMode mode, boolean leaveOpen, int length) {
        super(stream, mode, leaveOpen);
        this.length = length;
    }

    public long getLength() {
        return length;
    }

    @Override public long position() {
        return position;
    }

    @Override public void position(long value) {
        if (value != position()) {
            throw new UnsupportedOperationException();
        }
    }

    public int read(byte[] array, int offset, int count) {
        int read = super.read(array, offset, count);
        position += read;
        return read;
    }
}
