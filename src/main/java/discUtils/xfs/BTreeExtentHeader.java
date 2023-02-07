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

package discUtils.xfs;

import java.util.List;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public abstract class BTreeExtentHeader implements IByteArraySerializable {

    public static final int BtreeMagic = 0x424d4150;

    private int magic;

    public int getMagic() {
        return magic;
    }

    public void setMagic(int value) {
        magic = value;
    }

    private short level;

    public short getLevel() {
        return level;
    }

    public void setLevel(short value) {
        level = value;
    }

    private short numberOfRecords;

    public int getNumberOfRecords() {
        return numberOfRecords & 0xffff;
    }

    public void setNumberOfRecords(short value) {
        numberOfRecords = value;
    }

    private long leftSibling;

    public long getLeftSibling() {
        return leftSibling;
    }

    public void setLeftSibling(long value) {
        leftSibling = value;
    }

    private long rightSibling;

    public long getRightSibling() {
        return rightSibling;
    }

    public void setRightSibling(long value) {
        rightSibling = value;
    }

    @Override public int size() {
        return 24;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        magic = ByteUtil.readBeInt(buffer, offset);
        level = ByteUtil.readBeShort(buffer, offset + 0x4);
        numberOfRecords = ByteUtil.readBeShort(buffer, offset + 0x6);
        leftSibling = ByteUtil.readBeInt(buffer, offset + 0x8);
        rightSibling = ByteUtil.readBeInt(buffer, offset + 0xC);
        return 24;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public abstract void loadBtree(Context context);

    public abstract List<Extent> getExtents();
}
