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

import java.nio.charset.Charset;
import java.util.EnumSet;

import DiscUtils.Streams.Util.EndianUtilities;


public final class AttributeDefinitionRecord {
    public static final int Size = 0xA0;

    public AttributeCollationRule CollationRule = AttributeCollationRule.Binary;

    public int DisplayRule;

    public EnumSet<AttributeTypeFlags> Flags;

    public long MaxSize;

    public long MinSize;

    public String Name;

    public AttributeType Type = AttributeType.None;

    public void read(byte[] buffer, int offset) {
        Name = new String(buffer, offset + 0, 128, Charset.forName("UTF-16LE")).replaceFirst("^\0*", "")
                .replaceFirst("\0*$", "");
        Type = AttributeType.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x80));
        DisplayRule = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x84);
        CollationRule = AttributeCollationRule.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x88));
        Flags = AttributeTypeFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x8C));
        MinSize = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x90);
        MaxSize = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x98);
    }

    public void write(byte[] buffer, int offset) {
        byte[] bytes = Name.getBytes(Charset.forName("UTF-16LE"));
        System.arraycopy(bytes, 0, buffer, offset + 0, bytes.length);
        EndianUtilities.writeBytesLittleEndian(Type.ordinal(), buffer, offset + 0x80);
        EndianUtilities.writeBytesLittleEndian(DisplayRule, buffer, offset + 0x84);
        EndianUtilities.writeBytesLittleEndian(CollationRule.ordinal(), buffer, offset + 0x88);
        EndianUtilities.writeBytesLittleEndian((int) AttributeTypeFlags.valueOf(Flags), buffer, offset + 0x8C);
        EndianUtilities.writeBytesLittleEndian(MinSize, buffer, offset + 0x90);
        EndianUtilities.writeBytesLittleEndian(MaxSize, buffer, offset + 0x98);
    }
}
