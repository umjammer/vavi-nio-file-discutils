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

import java.util.EnumSet;
import java.util.UUID;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public final class MetadataEntry implements IByteArraySerializable {
    public EnumSet<MetadataEntryFlags> Flags;

    public UUID ItemId;

    public int Length;

    public int Offset;

    public int Reserved;

    public long getSize() {
        return 32;
    }

    public int readFrom(byte[] buffer, int offset) {
        ItemId = EndianUtilities.toGuidLittleEndian(buffer, offset + 0);
        Offset = EndianUtilities.toUInt32LittleEndian(buffer, offset + 16);
        Length = EndianUtilities.toUInt32LittleEndian(buffer, offset + 20);
        Flags = MetadataEntryFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 24));
        Reserved = EndianUtilities.toUInt32LittleEndian(buffer, offset + 28);
        return 32;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(ItemId, buffer, offset + 0);
        EndianUtilities.writeBytesLittleEndian(Offset, buffer, offset + 16);
        EndianUtilities.writeBytesLittleEndian(Length, buffer, offset + 20);
        EndianUtilities.writeBytesLittleEndian(MetadataEntryFlags.valueOf(Flags), buffer, offset + 24);
        EndianUtilities.writeBytesLittleEndian(Reserved, buffer, offset + 28);
    }

}
