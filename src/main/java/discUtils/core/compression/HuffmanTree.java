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
 * A canonical Huffman tree implementation.
 *
 * A lookup table is created that will take any bit sequence (max tree depth in
 * length), indicating the output symbol. In WIM files, in practice, no chunk
 * exceeds 32768 bytes in length, so we often end up generating a bigger lookup
 * table than the data it's encoding. This makes for exceptionally fast symbol
 * lookups O(1), but is inefficient overall.
 */
public final class HuffmanTree {

    private final int[] buffer;

    // Max bits per symbol
    private final int numBits;

    // Max symbols
    private final int numSymbols;

    public HuffmanTree(int[] lengths) {
        this.lengths = lengths;
        numSymbols = lengths.length;

        int maxLength = 0;
        for (int i = 0; i < getLengths().length; ++i) {
            if (getLengths()[i] > maxLength) {
                maxLength = getLengths()[i];
            }
        }

        numBits = maxLength;
        buffer = new int[1 << numBits];

        build();
    }

    private int[] lengths;

    public int[] getLengths() {
        return lengths;
    }

    public int nextSymbol(BitStream bitStream) {
        int symbol = buffer[bitStream.peek(numBits)];

        // We may have over-read, reset bitstream position
        bitStream.consume(getLengths()[symbol]);

        return symbol;
    }

    private void build() {
        int position = 0;

        // For each bit-length...
        for (int i = 1; i <= numBits; ++i) {
            // Check each symbol
            for (int symbol = 0; symbol < numSymbols; ++symbol) {
                if (getLengths()[symbol] == i) {
                    int numToFill = 1 << (numBits - i);
                    for (int n = 0; n < numToFill; ++n) {
                        buffer[position + n] = symbol;
                    }

                    position += numToFill;
                }
            }
        }

        for (int i = position; i < buffer.length; ++i) {
            buffer[i] = 0xffff_ffff;
        }
    }
}
