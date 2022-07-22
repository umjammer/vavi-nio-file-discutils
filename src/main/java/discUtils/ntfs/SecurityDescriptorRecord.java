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

package discUtils.ntfs;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public final class SecurityDescriptorRecord implements IByteArraySerializable {

    public int entrySize;

    public int hash;

    public int id;

    public long offsetInFile;

    public byte[] securityDescriptor;

    public int size() {
        return securityDescriptor.length + 0x14;
    }

    public int readFrom(byte[] buffer, int offset) {
        read(buffer, offset);
        return securityDescriptor.length + 0x14;
    }

    public void writeTo(byte[] buffer, int offset) {
        entrySize = size();
        EndianUtilities.writeBytesLittleEndian(hash, buffer, offset + 0x00);
        EndianUtilities.writeBytesLittleEndian(id, buffer, offset + 0x04);
        EndianUtilities.writeBytesLittleEndian(offsetInFile, buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(entrySize, buffer, offset + 0x10);
        System.arraycopy(securityDescriptor, 0, buffer, offset + 0x14, securityDescriptor.length);
    }

    public boolean read(byte[] buffer, int offset) {
        hash = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x00);
        id = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x04);
        offsetInFile = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x08);
        entrySize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x10);
        if (entrySize > 0) {
            securityDescriptor = new byte[entrySize - 0x14];
            System.arraycopy(buffer, offset + 0x14, securityDescriptor, 0, securityDescriptor.length);
            return true;
        }

        return false;
    }
}
