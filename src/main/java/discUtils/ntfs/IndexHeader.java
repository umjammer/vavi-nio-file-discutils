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

import vavi.util.ByteUtil;


public class IndexHeader {

    public static final int Size = 0x10;

    public int allocatedSizeOfEntries;

    public byte hasChildNodes;

    public int offsetToFirstEntry;

    public int totalSizeOfEntries;

    public IndexHeader(int allocatedSize) {
        allocatedSizeOfEntries = allocatedSize;
    }

    public IndexHeader(byte[] data, int offset) {
        offsetToFirstEntry = ByteUtil.readLeInt(data, offset + 0x00);
        totalSizeOfEntries = ByteUtil.readLeInt(data, offset + 0x04);
        allocatedSizeOfEntries = ByteUtil.readLeInt(data, offset + 0x08);
        hasChildNodes = data[offset + 0x0C];
    }

    public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeInt(offsetToFirstEntry, buffer, offset + 0x00);
        ByteUtil.writeLeInt(totalSizeOfEntries, buffer, offset + 0x04);
        ByteUtil.writeLeInt(allocatedSizeOfEntries, buffer, offset + 0x08);
        buffer[offset + 0x0C] = hasChildNodes;
        buffer[offset + 0x0D] = 0;
        buffer[offset + 0x0E] = 0;
        buffer[offset + 0x0F] = 0;
    }
}
