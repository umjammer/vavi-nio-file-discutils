//
// Copyright (c) 2008-2013, Kenneth Bell
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

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public final class LogEntryHeader implements IByteArraySerializable {

    public static final int LogEntrySignature = 0x65676F6C;

    private byte[] data;

    public int checksum;

    public int descriptorCount;

    public int entryLength;

    public long flushedFileOffset;

    public long lastFileOffset;

    public UUID logGuid;

    public int reserved;

    public long sequenceNumber;

    public int signature;

    public int tail;

    public boolean isValid() {
        return signature == LogEntrySignature;
    }

    public int size() {
        return 64;
    }

    public int readFrom(byte[] buffer, int offset) {
        data = new byte[size()];
        System.arraycopy(buffer, offset, data, 0, size());
        signature = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
        checksum = EndianUtilities.toUInt32LittleEndian(buffer, offset + 4);
        entryLength = EndianUtilities.toUInt32LittleEndian(buffer, offset + 8);
        tail = EndianUtilities.toUInt32LittleEndian(buffer, offset + 12);
        sequenceNumber = EndianUtilities.toUInt64LittleEndian(buffer, offset + 16);
        descriptorCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 24);
        reserved = EndianUtilities.toUInt32LittleEndian(buffer, offset + 28);
        logGuid = EndianUtilities.toGuidLittleEndian(buffer, offset + 32);
        flushedFileOffset = EndianUtilities.toUInt64LittleEndian(buffer, offset + 48);
        lastFileOffset = EndianUtilities.toUInt64LittleEndian(buffer, offset + 56);
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
