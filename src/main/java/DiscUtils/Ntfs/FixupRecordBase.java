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

package DiscUtils.Ntfs;

import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Sizes;
import dotnet4j.io.IOException;


public abstract class FixupRecordBase {
    private int _sectorSize;

    private short[] _updateSequenceArray;

    public FixupRecordBase(String magic, int sectorSize) {
        setMagic(magic);
        _sectorSize = sectorSize;
    }

    public FixupRecordBase(String magic, int sectorSize, int recordLength) {
        initialize(magic, sectorSize, recordLength);
    }

    private String _magic;

    public String getMagic() {
        return _magic;
    }

    public void setMagic(String value) {
        _magic = value;
    }

    public long getSize() {
        return calcSize();
    }

    private short _updateSequenceCount;

    public short getUpdateSequenceCount() {
        return _updateSequenceCount;
    }

    public void setUpdateSequenceCount(short value) {
        _updateSequenceCount = value;
    }

    private short _updateSequenceNumber;

    public short getUpdateSequenceNumber() {
        return _updateSequenceNumber;
    }

    public void setUpdateSequenceNumber(short value) {
        _updateSequenceNumber = value;
    }

    private short _updateSequenceOffset;

    public short getUpdateSequenceOffset() {
        return _updateSequenceOffset;
    }

    public void setUpdateSequenceOffset(short value) {
        assert value >= 0;
        _updateSequenceOffset = value;
    }

    public int getUpdateSequenceSize() {
        return getUpdateSequenceCount() * 2;
    }

    public void fromBytes(byte[] buffer, int offset) {
        fromBytes(buffer, offset, false);
    }

    public void fromBytes(byte[] buffer, int offset, boolean ignoreMagic) {
        String diskMagic = EndianUtilities.bytesToString(buffer, offset + 0x00, 4);
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
        setUpdateSequenceOffset(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x04));
        setUpdateSequenceCount(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x06));
        setUpdateSequenceNumber(EndianUtilities.toUInt16LittleEndian(buffer, offset + getUpdateSequenceOffset()));
        _updateSequenceArray = new short[getUpdateSequenceCount() - 1];
        for (int i = 0; i < _updateSequenceArray.length; ++i) {
            _updateSequenceArray[i] = EndianUtilities
                    .toUInt16LittleEndian(buffer, offset + getUpdateSequenceOffset() + 2 * (i + 1));
        }
        unprotectBuffer(buffer, offset);
        read(buffer, offset);
    }

    public void toBytes(byte[] buffer, int offset) {
        setUpdateSequenceOffset(write(buffer, offset));
        protectBuffer(buffer, offset);
        EndianUtilities.stringToBytes(getMagic(), buffer, offset + 0x00, 4);
        EndianUtilities.writeBytesLittleEndian(getUpdateSequenceOffset(), buffer, offset + 0x04);
        EndianUtilities.writeBytesLittleEndian(getUpdateSequenceCount(), buffer, offset + 0x06);
        EndianUtilities.writeBytesLittleEndian(getUpdateSequenceNumber(), buffer, offset + getUpdateSequenceOffset());
        for (int i = 0; i < _updateSequenceArray.length; ++i) {
            EndianUtilities
                    .writeBytesLittleEndian(_updateSequenceArray[i], buffer, offset + getUpdateSequenceOffset() + 2 * (i + 1));
        }
    }

    protected void initialize(String magic, int sectorSize, int recordLength) {
        setMagic(magic);
        _sectorSize = sectorSize;
        setUpdateSequenceCount((short) (1 + MathUtilities.ceil(recordLength, Sizes.Sector)));
        setUpdateSequenceNumber((short) 1);
        _updateSequenceArray = new short[getUpdateSequenceCount() - 1];
    }

    protected abstract void read(byte[] buffer, int offset);

    protected abstract short write(byte[] buffer, int offset);

    protected abstract int calcSize();

    private void unprotectBuffer(byte[] buffer, int offset) {
        for (int i = 0; i < _updateSequenceArray.length; ++i) {
            // First do validation check - make sure the USN matches on all sectors)
            if (getUpdateSequenceNumber() != EndianUtilities.toUInt16LittleEndian(buffer,
                                                                                  offset + Sizes.Sector * (i + 1) - 2)) {
                throw new IOException("Corrupt file system record found");
            }

        }
        for (int i = 0; i < _updateSequenceArray.length; ++i) {
            // Now replace the USNs with the actual data from the sequence array
            EndianUtilities.writeBytesLittleEndian(_updateSequenceArray[i], buffer, offset + Sizes.Sector * (i + 1) - 2);
        }
    }

    private void protectBuffer(byte[] buffer, int offset) {
        setUpdateSequenceNumber((short) (getUpdateSequenceNumber() + 1));
        for (int i = 0; i < _updateSequenceArray.length; ++i) {
            // Read in the bytes that are replaced by the USN
            _updateSequenceArray[i] = EndianUtilities.toUInt16LittleEndian(buffer, offset + Sizes.Sector * (i + 1) - 2);
        }
        for (int i = 0; i < _updateSequenceArray.length; ++i) {
            // Overwrite the bytes that are replaced with the USN
            EndianUtilities.writeBytesLittleEndian(getUpdateSequenceNumber(), buffer, offset + Sizes.Sector * (i + 1) - 2);
        }
    }
}
