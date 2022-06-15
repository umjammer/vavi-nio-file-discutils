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
public final class Crc32LittleEndian extends Crc32 {
    private static final int[][] Tables = new int[4][];
    static {
        Tables[Crc32Algorithm.Common.ordinal()] = calcTable(0xEDB88320);
        Tables[Crc32Algorithm.Castagnoli.ordinal()] = calcTable(0x82F63B78);
        Tables[Crc32Algorithm.Koopman.ordinal()] = calcTable(0xEB31D82E);
        Tables[Crc32Algorithm.Aeronautical.ordinal()] = calcTable(0xD5828281);
    }

    public Crc32LittleEndian(Crc32Algorithm algorithm) {
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
        table[0] = 0;
        for (int i = 0; i <= 255; ++i) {
            int crc = i;
            for (int j = 8; j > 0; --j) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ polynomial;
                } else {
                    crc >>>= 1;
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
            int temp1 = (value >>> 8) & 0x00FFFFFF;
            int temp2 = table[(value ^ b) & 0xFF];
            value = temp1 ^ temp2;
        }
        return value;
    }
}
