//
// Copyright (c) 2019, Bianco Veigel
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

import DiscUtils.Streams.Util.EndianUtilities;


public abstract class BTreeExtentHeaderV5 extends BTreeExtentHeader {
    public static final int BtreeMagicV5 = 0x424d4133;

    private long __BlockNumber;

    public long getBlockNumber() {
        return __BlockNumber;
    }

    public void setBlockNumber(long value) {
        __BlockNumber = value;
    }

    private long __LogSequenceNumber;

    public long getLogSequenceNumber() {
        return __LogSequenceNumber;
    }

    public void setLogSequenceNumber(long value) {
        __LogSequenceNumber = value;
    }

    private UUID __Uuid;

    public UUID getUuid() {
        return __Uuid;
    }

    public void setUuid(UUID value) {
        __Uuid = value;
    }

    private long __Owner;

    public long getOwner() {
        return __Owner;
    }

    public void setOwner(long value) {
        __Owner = value;
    }

    private int __Crc;

    public int getCrc() {
        return __Crc;
    }

    public void setCrc(int value) {
        __Crc = value;
    }

    public long getSize() {
        return super.getSize() + 48;
    }

    public int readFrom(byte[] buffer, int offset) {
        offset += super.readFrom(buffer, offset);
        setBlockNumber(EndianUtilities.toUInt64BigEndian(buffer, offset));
        setLogSequenceNumber(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x8));
        setUuid(EndianUtilities.toGuidBigEndian(buffer, offset + 0x10));
        setOwner(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x20));
        setCrc(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x28));
        return (int) super.getSize() + 48;
    }
}
