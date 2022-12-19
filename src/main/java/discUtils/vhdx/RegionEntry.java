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

import java.util.UUID;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public final class RegionEntry implements IByteArraySerializable {
    public static final UUID BatGuid = UUID.fromString("2dc27766-f623-4200-9d64-115e9bfd4a08");

    public static final UUID MetadataRegionGuid = UUID.fromString("8b7ca206-4790-4b9a-b8fe-575f050f886e");

    public long fileOffset;

    public RegionFlags flags;

    public UUID guid;

    private int length;

    public long getLength() {
        return length & 0xffffffffL;
    }

    public void setLength(int value) {
        length = value;
    }

    public int size() {
        return 32;
    }

    public int readFrom(byte[] buffer, int offset) {
        guid = ByteUtil.readLeUUID(buffer, offset + 0);
        fileOffset = ByteUtil.readLeLong(buffer, offset + 16);
        length = ByteUtil.readLeInt(buffer, offset + 24);
        flags = RegionFlags.values()[ByteUtil.readLeInt(buffer, offset + 28)];
        return 32;
    }

    public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeUUID(guid, buffer, offset + 0);
        ByteUtil.writeLeLong(fileOffset, buffer, offset + 16);
        ByteUtil.writeLeInt(length, buffer, offset + 24);
        ByteUtil.writeLeInt(flags.ordinal(), buffer, offset + 28);
    }

    public String toString() {
        return getClass().getSimpleName() + "@" + hashCode() + ": {offset: " + fileOffset + ", length: " + getLength() + "}";
    }
}
