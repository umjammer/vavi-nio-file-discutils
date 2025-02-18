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

public class MoveToFront {

    private final byte[] buffer;

    public MoveToFront() {
        this(256, false);
    }

    public MoveToFront(int size, boolean autoInit) {
        buffer = new byte[size];

        if (autoInit) {
            for (byte i = 0; i < size; ++i) {
                buffer[i] = i;
            }
        }
    }

    public byte getHead() {
        return buffer[0];
    }

    public void set(int pos, byte val) {
        buffer[pos] = val;
    }

    public byte getAndMove(int pos) {
        byte val = buffer[pos];

        System.arraycopy(buffer, 0, buffer, 1, pos);

        buffer[0] = val;
        return val;
    }
}
