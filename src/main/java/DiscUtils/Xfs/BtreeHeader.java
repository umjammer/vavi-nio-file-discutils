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

package DiscUtils.Xfs;

import java.util.UUID;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public abstract class BtreeHeader implements IByteArraySerializable {
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

    private int __LeftSibling;

    public int getLeftSibling() {
        return __LeftSibling;
    }

    public void setLeftSibling(int value) {
        __LeftSibling = value;
    }

    private int __RightSibling;

    public int getRightSibling() {
        return __RightSibling;
    }

    public void setRightSibling(int value) {
        __RightSibling = value;
    }

    /**
     * location on disk
     */
    private long __Bno;

    public long getBno() {
        return __Bno;
    }

    public void setBno(long value) {
        __Bno = value;
    }

    /**
     * last write sequence
     */
    private long __Lsn;

    public long getLsn() {
        return __Lsn;
    }

    public void setLsn(long value) {
        __Lsn = value;
    }

    private UUID __UniqueId;

    public UUID getUniqueId() {
        return __UniqueId;
    }

    public void setUniqueId(UUID value) {
        __UniqueId = value;
    }

    private int __Owner;

    public int getOwner() {
        return __Owner;
    }

    public void setOwner(int value) {
        __Owner = value;
    }

    private int __Crc;

    public int getCrc() {
        return __Crc;
    }

    public void setCrc(int value) {
        __Crc = value;
    }

    private int __Size;

    public long getSize() {
        return __Size;
    }

    private int __SbVersion;

    protected int getSbVersion() {
        return __SbVersion;
    }

    public BtreeHeader(int superBlockVersion) {
        __SbVersion = superBlockVersion;
        __Size = getSbVersion() >= 5 ? 56 : 16;
    }

    public int readFrom(byte[] buffer, int offset) {
        setMagic(EndianUtilities.toUInt32BigEndian(buffer, offset));
        setLevel(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x4));
        setNumberOfRecords(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x6));
        setLeftSibling(EndianUtilities.toInt32BigEndian(buffer, offset + 0x8));
        setRightSibling(EndianUtilities.toInt32BigEndian(buffer, offset + 0xC));
        if (getSbVersion() >= 5) {
            setBno(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x10));
            setLsn(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x18));
            setUniqueId(EndianUtilities.toGuidBigEndian(buffer, offset + 0x20));
            setOwner(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x30));
            setCrc(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x34));
        }

        return (int) getSize();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public abstract void loadBtree(AllocationGroup ag);
}
