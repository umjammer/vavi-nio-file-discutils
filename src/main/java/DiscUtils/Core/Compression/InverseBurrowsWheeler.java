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

import java.util.Arrays;


public final class InverseBurrowsWheeler extends DataBlockTransform {
    private final int[] _nextPos;

    private final int[] _pointers;

    public InverseBurrowsWheeler(int bufferSize) {
        _pointers = new int[bufferSize];
        _nextPos = new int[256];
    }

    protected boolean getBuffersMustNotOverlap() {
        return true;
    }

    private int __OriginalIndex;

    public int getOriginalIndex() {
        return __OriginalIndex;
    }

    public void setOriginalIndex(int value) {
        __OriginalIndex = value;
    }

    protected int doProcess(byte[] input, int inputOffset, int inputCount, byte[] output, int outputOffset) {
        int outputCount = inputCount;
        // First find the frequency of each value
        Arrays.fill(_nextPos, 0, _nextPos.length, 0);
        for (int i = inputOffset; i < inputOffset + inputCount; ++i) {
            _nextPos[input[i]]++;
        }
        // We know they're 'sorted' in the first column, so now can figure
        // out the position of the first instance of each.
        int sum = 0;
        for (int i = 0; i < 256; ++i) {
            int tempSum = sum;
            sum += _nextPos[i];
            _nextPos[i] = tempSum;
        }
        for (int i = 0; i < inputCount; ++i) {
            // For each value in the final column, put a pointer to to the
            // 'next' character in the first (sorted) column.
            _pointers[_nextPos[input[inputOffset + i]]++] = i;
        }
        // The 'next' character after the end of the original string is the
        // first character of the original string.
        int focus = _pointers[getOriginalIndex()];
        for (int i = 0; i < outputCount; ++i) {
            // We can now just walk the pointers to reconstruct the original string
            output[outputOffset + i] = input[inputOffset + focus];
            focus = _pointers[focus];
        }
        return outputCount;
    }

    protected int maxOutputCount(int inputCount) {
        return inputCount;
    }

    protected int minOutputCount(int inputCount) {
        return inputCount;
    }

}