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
// Contributed by bsobel:
//   - Derived from Puyo tools (BSD license)
//

package DiscUtils.Ntfs;

import java.util.ArrayList;
import java.util.List;


public final class LzWindowDictionary {
    /**
     * Index of locations of each possible byte value within the compression
     * window.
     */
    private final List<Integer>[] _offsetList;

    public LzWindowDictionary() {
        initalize();
        // Build the index list, so Lz compression will become significantly faster
        _offsetList = new ArrayList[0x100];
        for (int i = 0; i < _offsetList.length; i++) {
            _offsetList[i] = new ArrayList<>();
        }
    }

    private int __BlockSize;

    private int getBlockSize() {
        return __BlockSize;
    }

    private void setBlockSize(int value) {
        __BlockSize = value;
    }

    private int __MaxMatchAmount;

    public int getMaxMatchAmount() {
        return __MaxMatchAmount;
    }

    public void setMaxMatchAmount(int value) {
        __MaxMatchAmount = value;
    }

    private int __MinMatchAmount;

    public int getMinMatchAmount() {
        return __MinMatchAmount;
    }

    public void setMinMatchAmount(int value) {
        __MinMatchAmount = value;
    }

    public void reset() {
        initalize();
        for (int i = 0; i < _offsetList.length; i++) {
            _offsetList[i].clear();
        }
    }

    public int[] search(byte[] decompressedData, int decompressedDataOffset, int index, int length) {
        removeOldEntries(decompressedData[decompressedDataOffset + index]);
        // Remove old entries for this index
        int[] match = new int[] { 0, 0 };
        if (index < 1 || length - index < getMinMatchAmount()) {
            return match;
        }

        for (int i = 0; i < _offsetList[decompressedData[decompressedDataOffset + index]].size(); i++) {
            // Can't find matches if there isn't enough data
            int matchStart = _offsetList[decompressedData[decompressedDataOffset + index]].get(i);
            int matchSize = 1;
            if (index - matchStart > getBlockSize()) {
                break;
            }

            int maxMatchSize = Math.min(Math.min(getMaxMatchAmount(), getBlockSize()),
                                        Math.min(length - index, length - matchStart));
            while (matchSize < maxMatchSize &&
                   decompressedData[decompressedDataOffset + index + matchSize] == decompressedData[decompressedDataOffset +
                                                                                                    matchStart + matchSize]) {
                matchSize++;
            }
            if (matchSize >= getMinMatchAmount() && matchSize > match[1]) {
                // This is a good match
                match = new int[] {
                    index - matchStart, matchSize
                };
                if (matchSize == getMaxMatchAmount()) {
                    break;
                }
            }
        }
        return match;
    }

    // Don't look for more matches
    // Return the real match (or the default 0:0 match).
    // Add entries
    public void addEntry(byte[] decompressedData, int decompressedDataOffset, int index) {
        _offsetList[decompressedData[decompressedDataOffset + index]].add(index);
    }

    public void addEntryRange(byte[] decompressedData, int decompressedDataOffset, int index, int length) {
        for (int i = 0; i < length; i++) {
            addEntry(decompressedData, decompressedDataOffset, index + i);
        }
    }

    private void initalize() {
        setMinMatchAmount(3);
        setMaxMatchAmount(18);
        setBlockSize(4096);
    }

    private void removeOldEntries(byte index) {
        while (_offsetList[index].size() > 256) {
            _offsetList[index].remove(0);
        }
    }
}
