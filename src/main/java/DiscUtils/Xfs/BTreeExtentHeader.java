//
// Copyright (c) 2016, Bianco Veigel
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

package DiscUtils.Xfs;

import java.util.List;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public abstract class BTreeExtentHeader implements IByteArraySerializable {
    public static final int BtreeMagic = 0x424d4150;

    private int __Magic;

    public int getMagic() {
        return __Magic;
    }

    public void setMagic(int value) {
        __Magic = value;
    }

    private short __Level;

    public short getLevel() {
        return __Level;
    }

    public void setLevel(short value) {
        __Level = value;
    }

    private short __NumberOfRecords;

    public short getNumberOfRecords() {
        return __NumberOfRecords;
    }

    public void setNumberOfRecords(short value) {
        __NumberOfRecords = value;
    }

    private long __LeftSibling;

    public long getLeftSibling() {
        return __LeftSibling;
    }

    public void setLeftSibling(long value) {
        __LeftSibling = value;
    }

    private long __RightSibling;

    public long getRightSibling() {
        return __RightSibling;
    }

    public void setRightSibling(long value) {
        __RightSibling = value;
    }

    public long getSize() {
        return 24;
    }

    public int readFrom(byte[] buffer, int offset) {
        setMagic(EndianUtilities.toUInt32BigEndian(buffer, offset));
        setLevel(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x4));
        setNumberOfRecords(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x6));
        setLeftSibling(EndianUtilities.toInt64BigEndian(buffer, offset + 0x8));
        setRightSibling(EndianUtilities.toInt64BigEndian(buffer, offset + 0xC));
        return 24;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public abstract void loadBtree(Context context);

    public abstract List<Extent> getExtents();
}
