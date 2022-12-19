//
// Copyright (c) 2008-2011, Kenneth Bell
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

package discUtils.ntfs;

import java.nio.charset.StandardCharsets;

import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Sizes;
import dotnet4j.io.IOException;
import vavi.util.ByteUtil;


public abstract class FixupRecordBase {

    @SuppressWarnings("unused")
    private int sectorSize;

    private short[] updateSequenceArray;

    public FixupRecordBase(String magic, int sectorSize) {
        this.magic = magic;
        this.sectorSize = sectorSize;
    }

    public FixupRecordBase(String magic, int sectorSize, int recordLength) {
        initialize(magic, sectorSize, recordLength);
    }

    private String magic;

    public String getMagic() {
        return magic;
    }

    public void setMagic(String value) {
        magic = value;
    }

    public long getSize() {
        return calcSize();
    }

    private short updateSequenceCount;

    public int getUpdateSequenceCount() {
        return updateSequenceCount & 0xffff;
    }

    public void setUpdateSequenceCount(short value) {
        updateSequenceCount = value;
    }

    private short updateSequenceNumber;

    public int getUpdateSequenceNumber() {
        return updateSequenceNumber & 0xffff;
    }

    public void setUpdateSequenceNumber(short value) {
        updateSequenceNumber = value;
    }

    private short updateSequenceOffset;

    public int getUpdateSequenceOffset() {
        return updateSequenceOffset & 0xffff;
    }

    public void setUpdateSequenceOffset(short value) {
        updateSequenceOffset = value;
    }

    public int getUpdateSequenceSize() {
        return getUpdateSequenceCount() * 2;
    }

    public void fromBytes(byte[] buffer, int offset) {
        fromBytes(buffer, offset, false);
    }

    public void fromBytes(byte[] buffer, int offset, boolean ignoreMagic) {
        String diskMagic = new String(buffer, offset + 0x00, 4, StandardCharsets.US_ASCII);
        if (getMagic() == null) {
            setMagic(diskMagic);
        } else {
            if (!diskMagic.equals(getMagic()) && ignoreMagic) {
                return;
            }

            if (!diskMagic.equals(getMagic())) {
                throw new IOException("Corrupt record");
            }
        }

        setUpdateSequenceOffset(ByteUtil.readLeShort(buffer, offset + 0x04));
        setUpdateSequenceCount(ByteUtil.readLeShort(buffer, offset + 0x06));

        setUpdateSequenceNumber(ByteUtil.readLeShort(buffer, offset + getUpdateSequenceOffset()));
        updateSequenceArray = new short[getUpdateSequenceCount() - 1];
        for (int i = 0; i < updateSequenceArray.length; ++i) {
            updateSequenceArray[i] = ByteUtil.readLeShort(buffer,
                                                                           offset + getUpdateSequenceOffset() + 2 * (i + 1));
        }

        unprotectBuffer(buffer, offset);

        read(buffer, offset);
    }

    public void toBytes(byte[] buffer, int offset) {
        setUpdateSequenceOffset(write(buffer, offset));

        protectBuffer(buffer, offset);

        EndianUtilities.stringToBytes(getMagic(), buffer, offset + 0x00, 4);
        ByteUtil.writeLeShort(updateSequenceOffset, buffer, offset + 0x04);
        ByteUtil.writeLeShort(updateSequenceCount, buffer, offset + 0x06);

        ByteUtil.writeLeShort(updateSequenceNumber, buffer, offset + getUpdateSequenceOffset());
        for (int i = 0; i < updateSequenceArray.length; ++i) {
            ByteUtil.writeLeInt(updateSequenceArray[i], buffer, offset + getUpdateSequenceOffset() + 2 * (i + 1));
        }
    }

    protected void initialize(String magic, int sectorSize, int recordLength) {
        setMagic(magic);
        this.sectorSize = sectorSize;
        setUpdateSequenceCount((short) (1 + MathUtilities.ceil(recordLength, Sizes.Sector)));
        setUpdateSequenceNumber((short) 1);
        updateSequenceArray = new short[getUpdateSequenceCount() - 1];
    }

    protected abstract void read(byte[] buffer, int offset);

    protected abstract short write(byte[] buffer, int offset);

    protected abstract int calcSize();

    private void unprotectBuffer(byte[] buffer, int offset) {
        // First do validation check - make sure the USN matches on all sectors)
        for (int i = 0; i < updateSequenceArray.length; ++i) {
            if (updateSequenceNumber != ByteUtil.readLeShort(buffer, offset + Sizes.Sector * (i + 1) - 2)) {
                throw new IOException("Corrupt file system record found");
            }
        }

        // Now replace the USNs with the actual data from the sequence array
        for (int i = 0; i < updateSequenceArray.length; ++i) {
            ByteUtil.writeLeShort(updateSequenceArray[i], buffer, offset + Sizes.Sector * (i + 1) - 2);
        }
    }

    private void protectBuffer(byte[] buffer, int offset) {
        setUpdateSequenceNumber((short) (getUpdateSequenceNumber() + 1));

        // Read in the bytes that are replaced by the USN
        for (int i = 0; i < updateSequenceArray.length; ++i) {
            updateSequenceArray[i] = ByteUtil.readLeShort(buffer, offset + Sizes.Sector * (i + 1) - 2);
        }

        // Overwrite the bytes that are replaced with the USN
        for (int i = 0; i < updateSequenceArray.length; ++i) {
            ByteUtil.writeLeShort(updateSequenceNumber, buffer, offset + Sizes.Sector * (i + 1) - 2);
        }
    }
}
