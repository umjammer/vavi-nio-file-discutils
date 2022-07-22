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

package discUtils.registry;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import vavi.util.win32.DateUtil;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import dotnet4j.io.IOException;


public final class HiveHeader implements IByteArraySerializable {

    public static final int HeaderSize = 512;

    private static final int Signature = 0x66676572;

    public int checksum;

    public UUID guid1;

    public UUID guid2;

    public int length;

    public int majorVersion;

    public int minorVersion;

    public String path;

    public int rootCell;

    public int sequence1;

    public int sequence2;

    public long timestamp;

    public HiveHeader() {
        sequence1 = 1;
        sequence2 = 1;
        timestamp = System.currentTimeMillis();
        majorVersion = 1;
        minorVersion = 3;
        rootCell = -1;
        path = "";
        guid1 = UUID.randomUUID();
        guid2 = UUID.randomUUID();
    }

    public int size() {
        return HeaderSize;
    }

    public int readFrom(byte[] buffer, int offset) {
        int sig = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
        if (sig != Signature) {
            throw new IOException("Invalid signature for registry hive");
        }

        sequence1 = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x0004);
        sequence2 = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x0008);
        timestamp = DateUtil.toFileTime(EndianUtilities.toInt64LittleEndian(buffer, offset + 0x000C));
        majorVersion = EndianUtilities.toInt32LittleEndian(buffer, 0x0014);
        minorVersion = EndianUtilities.toInt32LittleEndian(buffer, 0x0018);
        @SuppressWarnings("unused")
        int isLog = EndianUtilities.toInt32LittleEndian(buffer, 0x001C);
        rootCell = EndianUtilities.toInt32LittleEndian(buffer, 0x0024);
        length = EndianUtilities.toInt32LittleEndian(buffer, 0x0028);
        path = new String(buffer, 0x0030, 0x0040, StandardCharsets.UTF_16LE).replaceFirst("^\0*", "").replaceFirst("\0*$", "");
        guid1 = EndianUtilities.toGuidLittleEndian(buffer, 0x0070);
        guid2 = EndianUtilities.toGuidLittleEndian(buffer, 0x0094);
        checksum = EndianUtilities.toUInt32LittleEndian(buffer, 0x01FC);
        if (sequence1 != sequence2) {
            throw new UnsupportedOperationException("Support for replaying registry log file");
        }

        if (checksum != calcChecksum(buffer, offset)) {
            throw new IOException("Invalid checksum on registry file");
        }

        return HeaderSize;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(Signature, buffer, offset);
        EndianUtilities.writeBytesLittleEndian(sequence1, buffer, offset + 0x0004);
        EndianUtilities.writeBytesLittleEndian(sequence2, buffer, offset + 0x0008);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(timestamp), buffer, offset + 0x000C);
        EndianUtilities.writeBytesLittleEndian(majorVersion, buffer, offset + 0x0014);
        EndianUtilities.writeBytesLittleEndian(minorVersion, buffer, offset + 0x0018);
        EndianUtilities.writeBytesLittleEndian(1, buffer, offset + 0x0020);
        // Unknown - seems to be '1'
        EndianUtilities.writeBytesLittleEndian(rootCell, buffer, offset + 0x0024);
        EndianUtilities.writeBytesLittleEndian(length, buffer, offset + 0x0028);
        byte[] bytes = path.getBytes(StandardCharsets.UTF_16LE);
        System.arraycopy(bytes, 0, buffer, offset + 0x0030, bytes.length);
        EndianUtilities.writeBytesLittleEndian((short) 0, buffer, offset + 0x0030 + path.length() * 2);
        EndianUtilities.writeBytesLittleEndian(guid1, buffer, offset + 0x0070);
        EndianUtilities.writeBytesLittleEndian(guid2, buffer, offset + 0x0094);
        EndianUtilities.writeBytesLittleEndian(calcChecksum(buffer, offset), buffer, offset + 0x01FC);
    }

    private static int calcChecksum(byte[] buffer, int offset) {
        int sum = 0;
        for (int i = 0; i < 0x01FC; i += 4) {
            sum = sum ^ EndianUtilities.toUInt32LittleEndian(buffer, offset + i);
        }
        return sum;
    }
}
