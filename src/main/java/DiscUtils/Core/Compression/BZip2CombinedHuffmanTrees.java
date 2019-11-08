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
//
// Based on "libbzip2", Copyright (C) 1996-2007 Julian R Seward.
//

package DiscUtils.Core.Compression;

/**
 * Represents scheme used by BZip2 where multiple Huffman trees are used as a
 * virtual Huffman tree, with a logical selector every 50 bits in the bit
 * stream.
 */
public class BZip2CombinedHuffmanTrees {
    private HuffmanTree _activeTree;

    private final BitStream _bitstream;

    private int _nextSelector;

    private byte[] _selectors;

    private int _symbolsToNextSelector;

    private HuffmanTree[] _trees;

    public BZip2CombinedHuffmanTrees(BitStream bitstream, int maxSymbols) {
        _bitstream = bitstream;

        initialize(maxSymbols);
    }

    public int nextSymbol() {
        if (_symbolsToNextSelector == 0) {
            _symbolsToNextSelector = 50;
            _activeTree = _trees[_selectors[_nextSelector]];
            _nextSelector++;
        }

        _symbolsToNextSelector--;

        return _activeTree.nextSymbol(_bitstream);
    }

    private void initialize(int maxSymbols) {
        int numTrees = _bitstream.read(3);
        if (numTrees < 2 || numTrees > 6) {
            throw new dotnet4j.io.IOException("Invalid number of tables");
        }

        int numSelectors = _bitstream.read(15);
        if (numSelectors < 1) {
            throw new dotnet4j.io.IOException("Invalid number of selectors");
        }

        _selectors = new byte[numSelectors];
        MoveToFront mtf = new MoveToFront(numTrees, true);
        for (int i = 0; i < numSelectors; ++i) {
            _selectors[i] = mtf.getAndMove(countSetBits(numTrees));
        }

        _trees = new HuffmanTree[numTrees];
        for (int t = 0; t < numTrees; ++t) {
            int[] lengths = new int[maxSymbols];

            int len = _bitstream.read(5);
            for (int i = 0; i < maxSymbols; ++i) {
                if (len < 1 || len > 20) {
                    throw new dotnet4j.io.IOException("Invalid length constructing Huffman tree");
                }

                while (_bitstream.read(1) != 0) {
                    len = _bitstream.read(1) == 0 ? len + 1 : len - 1;
                    if (len < 1 || len > 20) {
                        throw new dotnet4j.io.IOException("Invalid length constructing Huffman tree");
                    }

                }

                lengths[i] = len;
            }

            _trees[t] = new HuffmanTree(lengths);
        }

        _symbolsToNextSelector = 0;
        _nextSelector = 0;
    }

    private byte countSetBits(int max) {
        byte val = 0;
        while (_bitstream.read(1) != 0) {
            val++;
            if (val >= max) {
                throw new dotnet4j.io.IOException("Exceeded max number of consecutive bits");
            }
        }

        return val;
    }
}
