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

import org.bouncycastle.util.Arrays;

public abstract class DataBlockTransform {
    protected abstract boolean getBuffersMustNotOverlap();

    public int process(byte[] input, int inputOffset, int inputCount, byte[] output, int outputOffset) {
        if (output.length < outputOffset + (long) minOutputCount(inputCount)) {
            throw new IllegalArgumentException(String
                    .format("Output buffer to small, must be at least %d bytes may need to be %d bytes",
                            minOutputCount(inputCount),
                            maxOutputCount(inputCount)));
        }

        if (getBuffersMustNotOverlap()) {
            int maxOut = maxOutputCount(inputCount);
            if (Arrays.areEqual(input, output) && (inputOffset + (long) inputCount > outputOffset) &&
                (inputOffset <= outputOffset + (long) maxOut)) {
                byte[] tempBuffer = new byte[maxOut];
                int outCount = doProcess(input, inputOffset, inputCount, tempBuffer, 0);
                System.arraycopy(tempBuffer, 0, output, outputOffset, outCount);
                return outCount;
            }

        }

        return doProcess(input, inputOffset, inputCount, output, outputOffset);
    }

    protected abstract int doProcess(byte[] input,
                                     int inputOffset,
                                     int inputCount,
                                     byte[] output,
                                     int outputOffset);

    protected abstract int maxOutputCount(int inputCount);

    protected abstract int minOutputCount(int inputCount);

}
