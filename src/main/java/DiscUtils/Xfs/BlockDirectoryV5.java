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


public class BlockDirectoryV5 extends BlockDirectory {
    public static final int HeaderMagicV5 = 0x58444233;

    private int __Crc;

    public int getCrc() {
        return __Crc;
    }

    public void setCrc(int value) {
        __Crc = value;
    }

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

    protected int getHeaderPadding() {
        return 4;
    }

    public long getSize() {
        return 0x30 + 3 * 32 + 4;
    }

    public BlockDirectoryV5(Context context) {
        super(context);
    }

    public boolean getHasValidMagic() {
        return getMagic() == HeaderMagicV5;
    }

    protected int readHeader(byte[] buffer, int offset) {
        setMagic(EndianUtilities.toUInt32BigEndian(buffer, offset));
        setCrc(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x04));
        setBlockNumber(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x08));
        setLogSequenceNumber(EndianUtilities.toUInt64BigEndian(buffer, offset + 0x10));
        setUuid(EndianUtilities.toGuidBigEndian(buffer, offset + 0x18));
        setOwner(EndianUtilities.toUInt64BigEndian(buffer, 0x28));
        return 0x30;
    }
}
