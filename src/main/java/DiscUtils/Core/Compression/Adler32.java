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

package DiscUtils.Core.Compression;

/**
 * Implementation of the Adler-32 checksum algorithm.
 */
public class Adler32 {
    private int _a;

    private int _b;

    /**
     * Initializes a new instance of the Adler32 class.
     */
    public Adler32() {
        _a = 1;
    }

    /**
     * Gets the checksum of all data processed so far.
     */
    public int getValue() {
        return _b << 16 | _a;
    }

    /**
     * Provides data that should be checksummed.
     *
     * @param buffer Buffer containing the data to checksum.
     * @param offset Offset of the first byte to checksum.
     * @param count The number of bytes to checksum.
     *            Call this method repeatedly until all checksummed
     *            data has been processed.
     */
    public void process(byte[] buffer, int offset, int count) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer");
        }

        if (offset < 0 || offset > buffer.length) {
            throw new IllegalArgumentException("Offset outside of array bounds");
        }

        if (count < 0 || offset + count > buffer.length) {
            throw new IllegalArgumentException("Array index out of bounds");
        }

        int processed = 0;
        while (processed < count) {
            int innerEnd = Math.min(count, processed + 2000);
            while (processed < innerEnd) {
                _a += buffer[processed++];
                _b += _a;
            }
            _a %= 65521;
            _b %= 65521;
        }
    }

}
