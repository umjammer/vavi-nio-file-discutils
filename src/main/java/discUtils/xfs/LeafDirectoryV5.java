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

import vavi.util.ByteUtil;


public class LeafDirectoryV5 extends LeafDirectory {

    public static final int HeaderMagicV5 = 0x58444433;

    private int crc;

    public int getCrc() {
        return crc;
    }

    public void setCrc(int value) {
        crc = value;
    }

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

    @Override
    protected int getHeaderPadding() {
        return 4;
    }

    @Override
    public int size() {
        return 0x30 + 3 * 32 + getHeaderPadding();
    }

    public LeafDirectoryV5(Context context) {
        super(context);
    }

    @Override
    public boolean getHasValidMagic() {
        return getMagic() == HeaderMagicV5;
    }

    @Override
    protected int readHeader(byte[] buffer, int offset) {
        setMagic(ByteUtil.readBeInt(buffer, offset));
        crc = ByteUtil.readBeInt(buffer, offset + 0x04);
        blockNumber = ByteUtil.readBeLong(buffer, offset + 0x08);
        logSequenceNumber = ByteUtil.readBeLong(buffer, offset + 0x10);
        uuid = ByteUtil.readBeUUID(buffer, offset + 0x18);
        owner = ByteUtil.readBeLong(buffer, 0x28);
        return 0x30;
    }
}
