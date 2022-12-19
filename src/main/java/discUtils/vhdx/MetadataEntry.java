//
// Copyright (c) 2008-2012, Kenneth Bell
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

import java.util.EnumSet;
import java.util.UUID;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public final class MetadataEntry implements IByteArraySerializable {

    public EnumSet<MetadataEntryFlags> flags;

    public UUID itemId;

    public int length;

    public int offset;

    public int reserved;

    public int size() {
        return 32;
    }

    public int readFrom(byte[] buffer, int offset) {
        itemId = ByteUtil.readLeUUID(buffer, offset + 0);
        this.offset = ByteUtil.readLeInt(buffer, offset + 16);
        length = ByteUtil.readLeInt(buffer, offset + 20);
        flags = MetadataEntryFlags.valueOf(ByteUtil.readLeInt(buffer, offset + 24));
        reserved = ByteUtil.readLeInt(buffer, offset + 28);
        return 32;
    }

    public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeUUID(itemId, buffer, offset + 0);
        ByteUtil.writeLeInt(this.offset, buffer, offset + 16);
        ByteUtil.writeLeInt(length, buffer, offset + 20);
        ByteUtil.writeLeInt((int) MetadataEntryFlags.valueOf(flags), buffer, offset + 24);
        ByteUtil.writeLeInt(reserved, buffer, offset + 28);
    }
}
