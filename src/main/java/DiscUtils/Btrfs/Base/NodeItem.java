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

package DiscUtils.Btrfs.Base;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class NodeItem implements IByteArraySerializable {
    public static final int Length = Key.Length + 0x8;

    private Key __Key;

    public Key getKey() {
        return __Key;
    }

    public void setKey(Key value) {
        __Key = value;
    }

    private int __DataOffset;

    public int getDataOffset() {
        return __DataOffset;
    }

    public void setDataOffset(int value) {
        __DataOffset = value;
    }

    private int __DataSize;

    public int getDataSize() {
        return __DataSize;
    }

    public void setDataSize(int value) {
        __DataSize = value;
    }

    public int size() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        setKey(new Key());
        offset += getKey().readFrom(buffer, offset);
        setDataOffset(EndianUtilities.toUInt32LittleEndian(buffer, offset));
        setDataSize(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x4));
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        return getKey().toString();
    }
}
