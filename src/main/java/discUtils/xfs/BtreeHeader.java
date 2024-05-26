//
// Copyright (c) 2016, Bianco Veigel
// Copyright (c) 2017, Timo Walter
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

import java.util.UUID;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public abstract class BtreeHeader implements IByteArraySerializable {

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

    private int leftSibling;

    public int getLeftSibling() {
        return leftSibling;
    }

    public void setLeftSibling(int value) {
        leftSibling = value;
    }

    private int rightSibling;

    public int getRightSibling() {
        return rightSibling;
    }

    public void setRightSibling(int value) {
        rightSibling = value;
    }

    /**
     * location on disk
     */
    private long bno;

    public long getBno() {
        return bno;
    }

    public void setBno(long value) {
        bno = value;
    }

    /**
     * last write sequence
     */
    private long lsn;

    public long getLsn() {
        return lsn;
    }

    public void setLsn(long value) {
        lsn = value;
    }

    private UUID uniqueId;

    public UUID getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(UUID value) {
        uniqueId = value;
    }

    private int owner;

    public int getOwner() {
        return owner;
    }

    public void setOwner(int value) {
        owner = value;
    }

    private int crc;

    public int getCrc() {
        return crc;
    }

    public void setCrc(int value) {
        crc = value;
    }

    private final int size;

    @Override public int size() {
        return size;
    }

    private final int sbVersion;

    protected int getSbVersion() {
        return sbVersion;
    }

    public BtreeHeader(int superBlockVersion) {
        sbVersion = superBlockVersion;
        size = sbVersion >= 5 ? 56 : 16;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        magic = ByteUtil.readBeInt(buffer, offset);
        level = ByteUtil.readBeShort(buffer, offset + 0x4);
        numberOfRecords = ByteUtil.readBeShort(buffer, offset + 0x6);
        leftSibling = ByteUtil.readBeInt(buffer, offset + 0x8);
        rightSibling = ByteUtil.readBeInt(buffer, offset + 0xC);
        if (sbVersion >= 5) {
            bno = ByteUtil.readBeLong(buffer, offset + 0x10);
            lsn = ByteUtil.readBeLong(buffer, offset + 0x18);
            uniqueId = ByteUtil.readBeUUID(buffer, offset + 0x20);
            owner = ByteUtil.readBeInt(buffer, offset + 0x30);
            crc = ByteUtil.readBeInt(buffer, offset + 0x34);
        }

        return size();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public abstract void loadBtree(AllocationGroup ag);
}
