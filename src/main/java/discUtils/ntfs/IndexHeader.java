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

import discUtils.streams.util.EndianUtilities;


public class IndexHeader {
    public static final int Size = 0x10;

    public int _allocatedSizeOfEntries;

    public byte _hasChildNodes;

    public int _offsetToFirstEntry;

    public int _totalSizeOfEntries;

    public IndexHeader(int allocatedSize) {
        _allocatedSizeOfEntries = allocatedSize;
    }

    public IndexHeader(byte[] data, int offset) {
        _offsetToFirstEntry = EndianUtilities.toUInt32LittleEndian(data, offset + 0x00);
        _totalSizeOfEntries = EndianUtilities.toUInt32LittleEndian(data, offset + 0x04);
        _allocatedSizeOfEntries = EndianUtilities.toUInt32LittleEndian(data, offset + 0x08);
        _hasChildNodes = data[offset + 0x0C];
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(_offsetToFirstEntry, buffer, offset + 0x00);
        EndianUtilities.writeBytesLittleEndian(_totalSizeOfEntries, buffer, offset + 0x04);
        EndianUtilities.writeBytesLittleEndian(_allocatedSizeOfEntries, buffer, offset + 0x08);
        buffer[offset + 0x0C] = _hasChildNodes;
        buffer[offset + 0x0D] = 0;
        buffer[offset + 0x0E] = 0;
        buffer[offset + 0x0F] = 0;
    }
}
