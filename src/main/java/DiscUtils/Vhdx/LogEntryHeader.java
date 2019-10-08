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

package DiscUtils.Vhdx;

import java.util.UUID;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public final class LogEntryHeader implements IByteArraySerializable {
    public static final int LogEntrySignature = 0x65676F6C;

    private byte[] _data;

    public int Checksum;

    public int DescriptorCount;

    public int EntryLength;

    public long FlushedFileOffset;

    public long LastFileOffset;

    public UUID LogGuid;

    public int Reserved;

    public long SequenceNumber;

    public int Signature;

    public int Tail;

    public boolean isValid() {
        return Signature == LogEntrySignature;
    }

    public long getSize() {
        return 64;
    }

    public int readFrom(byte[] buffer, int offset) {
        _data = new byte[(int) getSize()];
        System.arraycopy(buffer, offset, _data, 0, (int) getSize());
        Signature = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
        Checksum = EndianUtilities.toUInt32LittleEndian(buffer, offset + 4);
        EntryLength = EndianUtilities.toUInt32LittleEndian(buffer, offset + 8);
        Tail = EndianUtilities.toUInt32LittleEndian(buffer, offset + 12);
        SequenceNumber = EndianUtilities.toUInt64LittleEndian(buffer, offset + 16);
        DescriptorCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 24);
        Reserved = EndianUtilities.toUInt32LittleEndian(buffer, offset + 28);
        LogGuid = EndianUtilities.toGuidLittleEndian(buffer, offset + 32);
        FlushedFileOffset = EndianUtilities.toUInt64LittleEndian(buffer, offset + 48);
        LastFileOffset = EndianUtilities.toUInt64LittleEndian(buffer, offset + 56);
        return (int) getSize();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
