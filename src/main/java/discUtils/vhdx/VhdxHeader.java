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

package discUtils.vhdx;

import java.util.UUID;

import discUtils.core.internal.Crc32Algorithm;
import discUtils.core.internal.Crc32LittleEndian;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Sizes;


public final class VhdxHeader implements IByteArraySerializable {

    public static final int VhdxHeaderSignature = 0x64616568;

    private final byte[] data = new byte[4096];

    public int checksum;

    public UUID fileWriteGuid;

    public UUID dataWriteGuid;

    public UUID logGuid;

    public int logLength;

    public long logOffset;

    public short logVersion;

    public long sequenceNumber;

    public int signature = VhdxHeaderSignature;

    public short version;

    public VhdxHeader() {
    }

    public VhdxHeader(VhdxHeader header) {
        System.arraycopy(header.data, 0, data, 0, 4096);
        signature = header.signature;
        checksum = header.checksum;
        sequenceNumber = header.sequenceNumber;
        fileWriteGuid = header.fileWriteGuid;
        dataWriteGuid = header.dataWriteGuid;
        logGuid = header.logGuid;
        logVersion = header.logVersion;
        version = header.version;
        logLength = header.logLength;
        logOffset = header.logOffset;
    }

    public boolean isValid() {
        if (signature != VhdxHeaderSignature) {
            return false;
        }

        byte[] checkData = new byte[4096];
        System.arraycopy(data, 0, checkData, 0, 4096);
        EndianUtilities.writeBytesLittleEndian(0, checkData, 4);
        return checksum == Crc32LittleEndian.compute(Crc32Algorithm.Castagnoli, checkData, 0, 4096);
    }

    public int size() {
        return (int) (4 * Sizes.OneKiB);
    }

    public int readFrom(byte[] buffer, int offset) {
        System.arraycopy(buffer, offset, data, 0, 4096);
        signature = EndianUtilities.toUInt32LittleEndian(data, 0);
        checksum = EndianUtilities.toUInt32LittleEndian(data, 4);
        sequenceNumber = EndianUtilities.toUInt64LittleEndian(data, 8);
        fileWriteGuid = EndianUtilities.toGuidLittleEndian(data, 16);
        dataWriteGuid = EndianUtilities.toGuidLittleEndian(data, 32);
        logGuid = EndianUtilities.toGuidLittleEndian(data, 48);
        logVersion = EndianUtilities.toUInt16LittleEndian(data, 64);
        version = EndianUtilities.toUInt16LittleEndian(data, 66);
        logLength = EndianUtilities.toUInt32LittleEndian(data, 68);
        logOffset = EndianUtilities.toUInt64LittleEndian(data, 72);
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        refreshData();
        System.arraycopy(data, 0, buffer, offset, (int) (4 * Sizes.OneKiB));
    }

    public void calcChecksum() {
        checksum = 0;
        refreshData();
        checksum = Crc32LittleEndian.compute(Crc32Algorithm.Castagnoli, data, 0, 4096);
    }

    private void refreshData() {
        EndianUtilities.writeBytesLittleEndian(signature, data, 0);
        EndianUtilities.writeBytesLittleEndian(checksum, data, 4);
        EndianUtilities.writeBytesLittleEndian(sequenceNumber, data, 8);
        EndianUtilities.writeBytesLittleEndian(fileWriteGuid, data, 16);
        EndianUtilities.writeBytesLittleEndian(dataWriteGuid, data, 32);
        EndianUtilities.writeBytesLittleEndian(logGuid, data, 48);
        EndianUtilities.writeBytesLittleEndian(logVersion, data, 64);
        EndianUtilities.writeBytesLittleEndian(version, data, 66);
        EndianUtilities.writeBytesLittleEndian(logLength, data, 68);
        EndianUtilities.writeBytesLittleEndian(logOffset, data, 72);
    }
}
