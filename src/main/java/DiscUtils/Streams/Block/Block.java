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

package DiscUtils.Streams.Block;

public class Block {
    private int __Available;

    public int getAvailable() {
        return __Available;
    }

    public void setAvailable(int value) {
        __Available = value;
    }

    private byte[] __Data;

    public byte[] getData() {
        return __Data;
    }

    public void setData(byte[] value) {
        __Data = value;
    }

    private long __Position;

    public long getPosition() {
        return __Position;
    }

    public void setPosition(long value) {
        __Position = value;
    }

    public boolean equals(Block other) {
        try {
            return getPosition() == other.getPosition();
        } catch (RuntimeException __dummyCatchVar0) {
            throw __dummyCatchVar0;
        } catch (Exception __dummyCatchVar0) {
            throw new RuntimeException(__dummyCatchVar0);
        }
    }
}