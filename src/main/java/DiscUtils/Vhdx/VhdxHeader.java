//
// Copyright (c) 2008-2012, Kenneth Bell
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

package DiscUtils.Vhdx;

import java.util.UUID;

import DiscUtils.Core.Internal.Crc32Algorithm;
import DiscUtils.Core.Internal.Crc32LittleEndian;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Sizes;


public final class VhdxHeader implements IByteArraySerializable {
    public static final int VhdxHeaderSignature = 0x64616568;

    private final byte[] _data = new byte[4096];

    public int Checksum;

    public UUID FileWriteGuid;

    public UUID DataWriteGuid;

    public UUID LogGuid;

    public int LogLength;

    public long LogOffset;

    public short LogVersion;

    public long SequenceNumber;

    public int Signature = VhdxHeaderSignature;

    public short Version;

    public VhdxHeader() {
    }

    public VhdxHeader(VhdxHeader header) {
        System.arraycopy(header._data, 0, _data, 0, 4096);
        Signature = header.Signature;
        Checksum = header.Checksum;
        SequenceNumber = header.SequenceNumber;
        FileWriteGuid = header.FileWriteGuid;
        DataWriteGuid = header.DataWriteGuid;
        LogGuid = header.LogGuid;
        LogVersion = header.LogVersion;
        Version = header.Version;
        LogLength = header.LogLength;
        LogOffset = header.LogOffset;
    }

    public boolean isValid() {
        if (Signature != VhdxHeaderSignature) {
            return false;
        }

        byte[] checkData = new byte[4096];
        System.arraycopy(_data, 0, checkData, 0, 4096);
        EndianUtilities.writeBytesLittleEndian(0, checkData, 4);
        return Checksum == Crc32LittleEndian.compute(Crc32Algorithm.Castagnoli, checkData, 0, 4096);
    }

    public long getSize() {
        return (int) (4 * Sizes.OneKiB);
    }

    public int readFrom(byte[] buffer, int offset) {
        System.arraycopy(buffer, offset, _data, 0, 4096);
        Signature = EndianUtilities.toUInt32LittleEndian(_data, 0);
        Checksum = EndianUtilities.toUInt32LittleEndian(_data, 4);
        SequenceNumber = EndianUtilities.toUInt64LittleEndian(_data, 8);
        FileWriteGuid = EndianUtilities.toGuidLittleEndian(_data, 16);
        DataWriteGuid = EndianUtilities.toGuidLittleEndian(_data, 32);
        LogGuid = EndianUtilities.toGuidLittleEndian(_data, 48);
        LogVersion = (short) EndianUtilities.toUInt16LittleEndian(_data, 64);
        Version = (short) EndianUtilities.toUInt16LittleEndian(_data, 66);
        LogLength = EndianUtilities.toUInt32LittleEndian(_data, 68);
        LogOffset = EndianUtilities.toUInt64LittleEndian(_data, 72);
        return (int) getSize();
    }

    public void writeTo(byte[] buffer, int offset) {
        refreshData();
        System.arraycopy(_data, 0, buffer, offset, (int) (4 * Sizes.OneKiB));
    }

    public void calcChecksum() {
        Checksum = 0;
        refreshData();
        Checksum = Crc32LittleEndian.compute(Crc32Algorithm.Castagnoli, _data, 0, 4096);
    }

    private void refreshData() {
        EndianUtilities.writeBytesLittleEndian(Signature, _data, 0);
        EndianUtilities.writeBytesLittleEndian(Checksum, _data, 4);
        EndianUtilities.writeBytesLittleEndian(SequenceNumber, _data, 8);
        EndianUtilities.writeBytesLittleEndian(FileWriteGuid, _data, 16);
        EndianUtilities.writeBytesLittleEndian(DataWriteGuid, _data, 32);
        EndianUtilities.writeBytesLittleEndian(LogGuid, _data, 48);
        EndianUtilities.writeBytesLittleEndian(LogVersion, _data, 64);
        EndianUtilities.writeBytesLittleEndian(Version, _data, 66);
        EndianUtilities.writeBytesLittleEndian(LogLength, _data, 68);
        EndianUtilities.writeBytesLittleEndian(LogOffset, _data, 72);
    }
}
