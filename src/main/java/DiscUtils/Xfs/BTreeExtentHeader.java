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

    private int _magic;

    public int getMagic() {
        return _magic;
    }

    public void setMagic(int value) {
        _magic = value;
    }

    private short _level;

    public short getLevel() {
        return _level;
    }

    public void setLevel(short value) {
        _level = value;
    }

    private short _numberOfRecords;

    public int getNumberOfRecords() {
        return _numberOfRecords & 0xffff;
    }

    public void setNumberOfRecords(short value) {
        _numberOfRecords = value;
    }

    private long _leftSibling;

    public long getLeftSibling() {
        return _leftSibling;
    }

    public void setLeftSibling(long value) {
        _leftSibling = value;
    }

    private long _rightSibling;

    public long getRightSibling() {
        return _rightSibling;
    }

    public void setRightSibling(long value) {
        _rightSibling = value;
    }

    public int size() {
        return 24;
    }

    public int readFrom(byte[] buffer, int offset) {
        _magic = EndianUtilities.toUInt32BigEndian(buffer, offset);
        _level = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x4);
        _numberOfRecords = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x6);
        _leftSibling = EndianUtilities.toInt64BigEndian(buffer, offset + 0x8);
        _rightSibling = EndianUtilities.toInt64BigEndian(buffer, offset + 0xC);
        return 24;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public abstract void loadBtree(Context context);

    public abstract List<Extent> getExtents();
}
