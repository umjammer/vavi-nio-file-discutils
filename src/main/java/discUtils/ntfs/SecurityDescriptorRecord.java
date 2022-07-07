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
    public int _entrySize;

    public int _hash;

    public int _id;

    public long _offsetInFile;

    public byte[] _securityDescriptor;

    public int size() {
        return _securityDescriptor.length + 0x14;
    }

    public int readFrom(byte[] buffer, int offset) {
        read(buffer, offset);
        return _securityDescriptor.length + 0x14;
    }

    public void writeTo(byte[] buffer, int offset) {
        _entrySize = size();
        EndianUtilities.writeBytesLittleEndian(_hash, buffer, offset + 0x00);
        EndianUtilities.writeBytesLittleEndian(_id, buffer, offset + 0x04);
        EndianUtilities.writeBytesLittleEndian(_offsetInFile, buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(_entrySize, buffer, offset + 0x10);
        System.arraycopy(_securityDescriptor, 0, buffer, offset + 0x14, _securityDescriptor.length);
    }

    public boolean read(byte[] buffer, int offset) {
        _hash = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x00);
        _id = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x04);
        _offsetInFile = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x08);
        _entrySize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x10);
        if (_entrySize > 0) {
            _securityDescriptor = new byte[_entrySize - 0x14];
            System.arraycopy(buffer, offset + 0x14, _securityDescriptor, 0, _securityDescriptor.length);
            return true;
        }

        return false;
    }
}
