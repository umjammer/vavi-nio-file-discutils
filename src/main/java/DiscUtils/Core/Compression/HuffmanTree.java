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
 * A canonical Huffman tree implementation.
 *
 * A lookup table is created that will take any bit sequence (max tree depth in
 * length), indicating the output symbol. In WIM files, in practice, no chunk
 * exceeds 32768 bytes in length, so we often end up generating a bigger lookup
 * table than the data it's encoding. This makes for exceptionally fast symbol
 * lookups O(1), but is inefficient overall.
 */
public final class HuffmanTree {
    private final int[] _buffer;

    private final int _numBits;

    // Max bits per symbol
    private final int _numSymbols;

    // Max symbols
    public HuffmanTree(int[] lengths) {
        __Lengths = lengths;
        _numSymbols = lengths.length;
        int maxLength = 0;
        for (int i = 0; i < getLengths().length; ++i) {
            if (getLengths()[i] > maxLength) {
                maxLength = getLengths()[i];
            }

        }
        _numBits = maxLength;
        _buffer = new int[1 << _numBits];
        build();
    }

    private int[] __Lengths;

    public int[] getLengths() {
        return __Lengths;
    }

    public int nextSymbol(BitStream bitStream) {
        int symbol = _buffer[bitStream.peek(_numBits)];
        // We may have over-read, reset bitstream position
        bitStream.consume(getLengths()[symbol]);
        return symbol;
    }

    private void build() {
        int position = 0;
        for (int i = 1; i <= _numBits; ++i) {
            for (int symbol = 0; symbol < _numSymbols; ++symbol) {
                // For each bit-length...
                // Check each symbol
                if (getLengths()[symbol] == i) {
                    int numToFill = 1 << (_numBits - i);
                    for (int n = 0; n < numToFill; ++n) {
                        _buffer[position + n] = symbol;
                    }
                    position += numToFill;
                }

            }
        }
        for (int i = position; i < _buffer.length; ++i) {
            _buffer[i] = 0xffffffff;
        }
    }
}
