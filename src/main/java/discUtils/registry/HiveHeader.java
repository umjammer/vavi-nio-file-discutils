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

import discUtils.streams.IByteArraySerializable;
import dotnet4j.io.IOException;
import vavi.util.ByteUtil;
import vavi.util.win32.DateUtil;


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
        int sig = ByteUtil.readLeInt(buffer, offset + 0);
        if (sig != Signature) {
            throw new IOException("Invalid signature for registry hive");
        }

        sequence1 = ByteUtil.readLeInt(buffer, offset + 0x0004);
        sequence2 = ByteUtil.readLeInt(buffer, offset + 0x0008);
        timestamp = DateUtil.toFileTime(ByteUtil.readLeLong(buffer, offset + 0x000C));
        majorVersion = ByteUtil.readLeInt(buffer, 0x0014);
        minorVersion = ByteUtil.readLeInt(buffer, 0x0018);
        @SuppressWarnings("unused")
        int isLog = ByteUtil.readLeInt(buffer, 0x001C);
        rootCell = ByteUtil.readLeInt(buffer, 0x0024);
        length = ByteUtil.readLeInt(buffer, 0x0028);
        path = new String(buffer, 0x0030, 0x0040, StandardCharsets.UTF_16LE).replaceFirst("^\0*", "").replaceFirst("\0*$", "");
        guid1 = ByteUtil.readLeUUID(buffer, 0x0070);
        guid2 = ByteUtil.readLeUUID(buffer, 0x0094);
        checksum = ByteUtil.readLeInt(buffer, 0x01FC);
        if (sequence1 != sequence2) {
            throw new UnsupportedOperationException("Support for replaying registry log file");
        }

        if (checksum != calcChecksum(buffer, offset)) {
            throw new IOException("Invalid checksum on registry file");
        }

        return HeaderSize;
    }

    public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeInt(Signature, buffer, offset);
        ByteUtil.writeLeInt(sequence1, buffer, offset + 0x0004);
        ByteUtil.writeLeInt(sequence2, buffer, offset + 0x0008);
        ByteUtil.writeLeLong(DateUtil.toFileTime(timestamp), buffer, offset + 0x000C);
        ByteUtil.writeLeInt(majorVersion, buffer, offset + 0x0014);
        ByteUtil.writeLeInt(minorVersion, buffer, offset + 0x0018);
        ByteUtil.writeLeInt(1, buffer, offset + 0x0020);
        // Unknown - seems to be '1'
        ByteUtil.writeLeInt(rootCell, buffer, offset + 0x0024);
        ByteUtil.writeLeInt(length, buffer, offset + 0x0028);
        byte[] bytes = path.getBytes(StandardCharsets.UTF_16LE);
        System.arraycopy(bytes, 0, buffer, offset + 0x0030, bytes.length);
        ByteUtil.writeLeShort((short) 0, buffer, offset + 0x0030 + path.length() * 2);
        ByteUtil.writeLeUUID(guid1, buffer, offset + 0x0070);
        ByteUtil.writeLeUUID(guid2, buffer, offset + 0x0094);
        ByteUtil.writeLeInt(calcChecksum(buffer, offset), buffer, offset + 0x01FC);
    }

    private static int calcChecksum(byte[] buffer, int offset) {
        int sum = 0;
        for (int i = 0; i < 0x01FC; i += 4) {
            sum = sum ^ ByteUtil.readLeInt(buffer, offset + i);
        }
        return sum;
    }
}
