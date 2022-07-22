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

package discUtils.xfs;

import java.util.UUID;

import discUtils.streams.util.EndianUtilities;


public abstract class BTreeExtentHeaderV5 extends BTreeExtentHeader {

    public static final int BtreeMagicV5 = 0x424d4133;

    private long blockNumber;

    public long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(long value) {
        blockNumber = value;
    }

    private long logSequenceNumber;

    public long getLogSequenceNumber() {
        return logSequenceNumber;
    }

    public void setLogSequenceNumber(long value) {
        logSequenceNumber = value;
    }

    private UUID uuid;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID value) {
        uuid = value;
    }

    private long owner;

    public long getOwner() {
        return owner;
    }

    public void setOwner(long value) {
        owner = value;
    }

    private int crc;

    public int getCrc() {
        return crc;
    }

    public void setCrc(int value) {
        crc = value;
    }

    public int size() {
        return super.size() + 48;
    }

    public int readFrom(byte[] buffer, int offset) {
        offset += super.readFrom(buffer, offset);
        blockNumber = EndianUtilities.toUInt64BigEndian(buffer, offset);
        logSequenceNumber = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x8);
        uuid = EndianUtilities.toGuidBigEndian(buffer, offset + 0x10);
        owner = EndianUtilities.toUInt64BigEndian(buffer, offset + 0x20);
        crc = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x28);
        return super.size() + 48;
    }
}
