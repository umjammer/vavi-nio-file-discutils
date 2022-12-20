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
import discUtils.streams.util.Sizes;
import vavi.util.ByteUtil;


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
        ByteUtil.writeLeInt(0, checkData, 4);
        return checksum == Crc32LittleEndian.compute(Crc32Algorithm.Castagnoli, checkData, 0, 4096);
    }

    @Override public int size() {
        return (int) (4 * Sizes.OneKiB);
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        System.arraycopy(buffer, offset, data, 0, 4096);
        signature = ByteUtil.readLeInt(data, 0);
        checksum = ByteUtil.readLeInt(data, 4);
        sequenceNumber = ByteUtil.readLeLong(data, 8);
        fileWriteGuid = ByteUtil.readLeUUID(data, 16);
        dataWriteGuid = ByteUtil.readLeUUID(data, 32);
        logGuid = ByteUtil.readLeUUID(data, 48);
        logVersion = ByteUtil.readLeShort(data, 64);
        version = ByteUtil.readLeShort(data, 66);
        logLength = ByteUtil.readLeInt(data, 68);
        logOffset = ByteUtil.readLeLong(data, 72);
        return size();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        refreshData();
        System.arraycopy(data, 0, buffer, offset, (int) (4 * Sizes.OneKiB));
    }

    public void calcChecksum() {
        checksum = 0;
        refreshData();
        checksum = Crc32LittleEndian.compute(Crc32Algorithm.Castagnoli, data, 0, 4096);
    }

    private void refreshData() {
        ByteUtil.writeLeInt(signature, data, 0);
        ByteUtil.writeLeInt(checksum, data, 4);
        ByteUtil.writeLeLong(sequenceNumber, data, 8);
        ByteUtil.writeLeUUID(fileWriteGuid, data, 16);
        ByteUtil.writeLeUUID(dataWriteGuid, data, 32);
        ByteUtil.writeLeUUID(logGuid, data, 48);
        ByteUtil.writeLeShort(logVersion, data, 64);
        ByteUtil.writeLeShort(version, data, 66);
        ByteUtil.writeLeInt(logLength, data, 68);
        ByteUtil.writeLeLong(logOffset, data, 72);
    }
}
