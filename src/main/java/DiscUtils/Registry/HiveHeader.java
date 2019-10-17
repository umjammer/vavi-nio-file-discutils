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

package DiscUtils.Registry;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.UUID;

import vavi.util.win32.DateUtil;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import moe.yo3explorer.dotnetio4j.IOException;


public final class HiveHeader implements IByteArraySerializable {
    public static final int HeaderSize = 512;

    private static final int Signature = 0x66676572;

    public int Checksum;

    public UUID Guid1;

    public UUID Guid2;

    public int Length;

    public int MajorVersion;

    public int MinorVersion;

    public String Path;

    public int RootCell;

    public int Sequence1;

    public int Sequence2;

    public long Timestamp;

    public HiveHeader() {
        Sequence1 = 1;
        Sequence2 = 1;
        Timestamp = System.currentTimeMillis();
        MajorVersion = 1;
        MinorVersion = 3;
        RootCell = -1;
        Path = "";
        Guid1 = UUID.randomUUID();
        Guid2 = UUID.randomUUID();
    }

    public long getSize() {
        return HeaderSize;
    }

    public int readFrom(byte[] buffer, int offset) {
        int sig = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
        if (sig != Signature) {
            throw new IOException("Invalid signature for registry hive");
        }

        Sequence1 = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x0004);
        Sequence2 = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x0008);
        Timestamp = DateUtil.filetimeToLong(EndianUtilities.toInt64LittleEndian(buffer, offset + 0x000C));
        MajorVersion = EndianUtilities.toInt32LittleEndian(buffer, 0x0014);
        MinorVersion = EndianUtilities.toInt32LittleEndian(buffer, 0x0018);
        @SuppressWarnings("unused")
        int isLog = EndianUtilities.toInt32LittleEndian(buffer, 0x001C);
        RootCell = EndianUtilities.toInt32LittleEndian(buffer, 0x0024);
        Length = EndianUtilities.toInt32LittleEndian(buffer, 0x0028);
        Path = new String(buffer, 0x0030, 0x0040, Charset.forName("UTF-16LE")).replaceFirst("^\0*", "").replaceFirst("\0*$", "");
        Guid1 = EndianUtilities.toGuidLittleEndian(buffer, 0x0070);
        Guid2 = EndianUtilities.toGuidLittleEndian(buffer, 0x0094);
        Checksum = EndianUtilities.toUInt32LittleEndian(buffer, 0x01FC);
        if (Sequence1 != Sequence2) {
            throw new UnsupportedOperationException("Support for replaying registry log file");
        }

        if (Checksum != calcChecksum(buffer, offset)) {
            throw new IOException("Invalid checksum on registry file");
        }

        return HeaderSize;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(Signature, buffer, offset);
        EndianUtilities.writeBytesLittleEndian(Sequence1, buffer, offset + 0x0004);
        EndianUtilities.writeBytesLittleEndian(Sequence2, buffer, offset + 0x0008);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(Instant.ofEpochMilli(Timestamp)), buffer, offset + 0x000C);
        EndianUtilities.writeBytesLittleEndian(MajorVersion, buffer, offset + 0x0014);
        EndianUtilities.writeBytesLittleEndian(MinorVersion, buffer, offset + 0x0018);
        EndianUtilities.writeBytesLittleEndian(1, buffer, offset + 0x0020);
        // Unknown - seems to be '1'
        EndianUtilities.writeBytesLittleEndian(RootCell, buffer, offset + 0x0024);
        EndianUtilities.writeBytesLittleEndian(Length, buffer, offset + 0x0028);
        byte[] bytes = Path.getBytes(Charset.forName("UTF-16LE"));
        System.arraycopy(bytes, 0, buffer, offset + 0x0030, bytes.length);
        EndianUtilities.writeBytesLittleEndian((short) 0, buffer, offset + 0x0030 + Path.length() * 2);
        EndianUtilities.writeBytesLittleEndian(Guid1, buffer, offset + 0x0070);
        EndianUtilities.writeBytesLittleEndian(Guid2, buffer, offset + 0x0094);
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
