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

package DiscUtils.Vhdx;

import java.util.UUID;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public final class RegionEntry implements IByteArraySerializable {
    public static final UUID BatGuid = UUID.fromString("2dc27766-f623-4200-9d64-115e9bfd4a08");

    public static final UUID MetadataRegionGuid = UUID.fromString("8b7ca206-4790-4b9a-b8fe-575f050f886e");

    public long FileOffset;

    public RegionFlags Flags;

    public UUID Guid;

    private int Length;

    public long getLength() {
        return Length & 0xffffffffl;
    }

    public void setLength(int value) {
        Length = value;
    }

    public long getSize() {
        return 32;
    }

    public int readFrom(byte[] buffer, int offset) {
        Guid = EndianUtilities.toGuidLittleEndian(buffer, offset + 0);
        FileOffset = EndianUtilities.toInt64LittleEndian(buffer, offset + 16);
        Length = EndianUtilities.toUInt32LittleEndian(buffer, offset + 24);
        Flags = RegionFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 28));
        return 32;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(Guid, buffer, offset + 0);
        EndianUtilities.writeBytesLittleEndian(FileOffset, buffer, offset + 16);
        EndianUtilities.writeBytesLittleEndian(Length, buffer, offset + 24);
        EndianUtilities.writeBytesLittleEndian(Flags.ordinal(), buffer, offset + 28);
    }

}
