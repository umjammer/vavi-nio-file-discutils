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
import discUtils.streams.util.EndianUtilities;


public abstract class BtreeHeader implements IByteArraySerializable {
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

    private int _leftSibling;

    public int getLeftSibling() {
        return _leftSibling;
    }

    public void setLeftSibling(int value) {
        _leftSibling = value;
    }

    private int _rightSibling;

    public int getRightSibling() {
        return _rightSibling;
    }

    public void setRightSibling(int value) {
        _rightSibling = value;
    }

    /**
     * location on disk
     */
    private long _bno;

    public long getBno() {
        return _bno;
    }

    public void setBno(long value) {
        _bno = value;
    }

    /**
     * last write sequence
     */
    private long _lsn;

    public long getLsn() {
        return _lsn;
    }

    public void setLsn(long value) {
        _lsn = value;
    }

    private UUID _uniqueId;

    public UUID getUniqueId() {
        return _uniqueId;
    }

    public void setUniqueId(UUID value) {
        _uniqueId = value;
    }

    private int _owner;

    public int getOwner() {
        return _owner;
    }

    public void setOwner(int value) {
        _owner = value;
    }

    private int _crc;

    public int getCrc() {
        return _crc;
    }

    public void setCrc(int value) {
        _crc = value;
    }

    private int _size;

    public int size() {
        return _size;
    }

    private int _sbVersion;

    protected int getSbVersion() {
        return _sbVersion;
    }

    public BtreeHeader(int superBlockVersion) {
        _sbVersion = superBlockVersion;
        _size = getSbVersion() >= 5 ? 56 : 16;
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

        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public abstract void loadBtree(AllocationGroup ag);
}
