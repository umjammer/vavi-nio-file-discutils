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

package DiscUtils.Core.Internal;

/**
 * Calculates CRC32 of buffers.
 */
public final class Crc32BigEndian extends Crc32 {
    private static final int[][] Tables;
    static {
        Tables = new int[4][];
        Tables[Crc32Algorithm.Common.ordinal()] = calcTable(0x04C11DB7);
        Tables[Crc32Algorithm.Castagnoli.ordinal()] = calcTable(0x1EDC6F41);
        Tables[Crc32Algorithm.Koopman.ordinal()] = calcTable(0x741B8CD7);
        Tables[Crc32Algorithm.Aeronautical.ordinal()] = calcTable(0x814141AB);
    }

    public Crc32BigEndian(Crc32Algorithm algorithm) {
        super(Tables[algorithm.ordinal()]);
    }

    public static int compute(Crc32Algorithm algorithm, byte[] buffer, int offset, int count) {
        return ~process(Tables[algorithm.ordinal()], 0xFFFFFFFF, buffer, offset, count);
    }

    public void process(byte[] buffer, int offset, int count) {
        value = process(table, value, buffer, offset, count);
    }

    private static int[] calcTable(int polynomial) {
        int[] table = new int[256];
        for (int i = 0; i < 256; ++i) {
            int crc = i << 24;
            for (int j = 8; j > 0; --j) {
                if ((crc & 0x80000000) != 0) {
                    crc = (crc << 1) ^ polynomial;
                } else {
                    crc <<= 1;
                }
            }
            table[i] = crc;
        }
        return table;
    }

    private static int process(int[] table, int accumulator, byte[] buffer, int offset, int count) {
        int value = accumulator;
        for (int i = 0; i < count; ++i) {
            byte b = buffer[offset + i];
            value = table[(value >>> 24) ^ b] ^ (value << 8);
        }
        return value;
    }

}
