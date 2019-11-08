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

package DiscUtils.Core.LogicalDiskManager;

import java.util.UUID;

import DiscUtils.Streams.Util.EndianUtilities;


public final class VolumeRecord extends DatabaseRecord {
    public String ActiveString;

    public byte BiosType;

    public long ComponentCount;

    // ??Seen once after adding 'foreign disk', from broken mirror (identical links(P/V/C))
    public long DupCount;

    public String GenString;

    public String MountHint;

    // 8000000000000000 sometimes...
    public String NumberString;

    public int PartitionComponentLink;

    public long Size;

    // Zero
    public long Unknown1;

    // Zero
    public int Unknown2;

    // Zero
    public long UnknownA;

    // 00 .. 03
    public long UnknownB;

    // 00 00 00 11
    public int UnknownC;

    // Zero
    public int UnknownD;

    public UUID VolumeGuid;

    protected void doReadFrom(byte[] buffer, int offset) {
        super.doReadFrom(buffer, offset);

        int[] pos = new int[] { offset + 0x18 };

        Id = readVarULong(buffer, pos);
        Name = readVarString(buffer, pos);
        GenString = readVarString(buffer, pos);
        NumberString = readVarString(buffer, pos);
        ActiveString = readString(buffer, 6, pos);
        UnknownA = readVarULong(buffer, pos);
        UnknownB = readULong(buffer, pos);
        DupCount = readVarULong(buffer, pos);
        UnknownC = readUInt(buffer, pos);
        ComponentCount = readVarULong(buffer, pos);
        UnknownD = readUInt(buffer, pos);
        PartitionComponentLink = readUInt(buffer, pos);
        Unknown1 = readULong(buffer, pos);
        Size = readVarLong(buffer, pos);
        Unknown2 = readUInt(buffer, pos);
        BiosType = readByte(buffer, pos);
        VolumeGuid = EndianUtilities.toGuidBigEndian(buffer, pos[0]);
        pos[0] += 16;

        if ((Flags & 0x0200) != 0) {
            MountHint = readVarString(buffer, pos);
        }
    }
}
