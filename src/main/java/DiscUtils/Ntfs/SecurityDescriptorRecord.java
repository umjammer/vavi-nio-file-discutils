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

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public final class SecurityDescriptorRecord implements IByteArraySerializable {
    public int EntrySize;

    public int Hash;

    public int Id;

    public long OffsetInFile;

    public byte[] SecurityDescriptor;

    public int sizeOf() {
        return SecurityDescriptor.length + 0x14;
    }

    public int readFrom(byte[] buffer, int offset) {
        read(buffer, offset);
        return SecurityDescriptor.length + 0x14;
    }

    public void writeTo(byte[] buffer, int offset) {
        EntrySize = sizeOf();
        EndianUtilities.writeBytesLittleEndian(Hash, buffer, offset + 0x00);
        EndianUtilities.writeBytesLittleEndian(Id, buffer, offset + 0x04);
        EndianUtilities.writeBytesLittleEndian(OffsetInFile, buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(EntrySize, buffer, offset + 0x10);
        System.arraycopy(SecurityDescriptor, 0, buffer, offset + 0x14, SecurityDescriptor.length);
    }

    public boolean read(byte[] buffer, int offset) {
        Hash = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x00);
        Id = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x04);
        OffsetInFile = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x08);
        EntrySize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x10);
        if (EntrySize > 0) {
            SecurityDescriptor = new byte[EntrySize - 0x14];
            System.arraycopy(buffer, offset + 0x14, SecurityDescriptor, 0, SecurityDescriptor.length);
            return true;
        }

        return false;
    }
}
