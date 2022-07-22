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

package discUtils.core.compression;

/**
 * Implementation of the Adler-32 checksum algorithm.
 */
public class Adler32 {

    private long a;

    private long b;

    /**
     * Initializes a new instance of the Adler32 class.
     */
    public Adler32() {
        a = 1;
    }

    /**
     * Gets the checksum of all data processed so far.
     */
    public int getValue() {
        return (int) (b << 16 | a);
    }

    /**
     * Provides data that should be checksummed.
     *
     * Call this method repeatedly until all checksummed data has been
     * processed.
     *
     * @param buffer buffer containing the data to checksum.
     * @param offset Offset of the first byte to checksum.
     * @param count The number of bytes to checksum.
     */
    public void process(byte[] buffer, int offset, int count) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }

        if (offset < 0 || offset > buffer.length) {
            throw new IllegalArgumentException("offset: Offset outside of array bounds: " + offset);
        }

        if (count < 0 || offset + count > buffer.length) {
            throw new IllegalArgumentException("count: Array index out of bounds: " + count);
        }

        int processed = 0;
        while (processed < count) {
            int innerEnd = Math.min(count, processed + 2000);
            while (processed < innerEnd) {
                a += buffer[processed++] & 0xff;
                b += a;
            }
            a %= 65521;
            b %= 65521;
        }
    }
}
