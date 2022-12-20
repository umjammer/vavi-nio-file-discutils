//
// Copyright (c) 2017, Bianco Veigel
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

package discUtils.btrfs.base;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class NodeItem implements IByteArraySerializable {

    public static final int Length = Key.Length + 0x8;

    private Key key;

    public Key getKey() {
        return key;
    }

    public void setKey(Key value) {
        key = value;
    }

    private int dataOffset;

    public int getDataOffset() {
        return dataOffset;
    }

    public void setDataOffset(int value) {
        dataOffset = value;
    }

    private int dataSize;

    public int getDataSize() {
        return dataSize;
    }

    public void setDataSize(int value) {
        dataSize = value;
    }

    @Override public int size() {
        return Length;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        key = new Key();
        offset += getKey().readFrom(buffer, offset);
        dataOffset = ByteUtil.readLeInt(buffer, offset);
        dataSize = ByteUtil.readLeInt(buffer, offset + 0x4);
        return size();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override public String toString() {
        return key.toString();
    }
}
