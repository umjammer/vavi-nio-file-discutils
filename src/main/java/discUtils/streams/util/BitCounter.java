//
// Copyright (c) 2017, Bianco Veigel
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

package discUtils.streams.util;


/**
 * Helper to count the number of bits set in a byte or byte[]
 */
public class BitCounter {

    private static final byte[] lookupTable;

    static {
        lookupTable = new byte[256];
        for (int i = 0; i < 256; i++) {
            byte bitCount = 0;
            int value = i;
            while (value != 0) {
                bitCount++;
                value &= (byte) (value - 1);
            }
            lookupTable[i] = bitCount;
        }
    }

    /**
     * count the number of bits set in
     * {@code value}
     *
     * @return the number of bits set in
     *         {@code value}
     */
    public static byte count(byte value) {
        return lookupTable[value];
    }

    /**
     * count the number of bits set in each entry of
     * {@code values}
     *
     * @param values the array to process
     * @param offset the values offset to start from
     * @param count the number of bytes to count
     * @return
     */
    public static long count(byte[] values, int offset, int count) {
        int end = offset + count;
        if (end > values.length)
            throw new IndexOutOfBoundsException("can't count after end of values");

        long result = 0L;
        for (int i = offset; i < end; i++) {
            int value = values[i] & 0xff;
            result += lookupTable[value];
        }
        return result;
    }
}
