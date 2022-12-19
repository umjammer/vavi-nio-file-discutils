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

package discUtils.core.logicalDiskManager;

import java.util.UUID;

import vavi.util.ByteUtil;


public final class VolumeRecord extends DatabaseRecord {

    public String activeString;

    public byte biosType;

    public long componentCount;

    /** ??Seen once after adding 'foreign disk', from broken mirror (identical links(P/V/C)) */
    public long dupCount;

    public String genString;

    public String mountHint;

    /** 8000000000000000 sometimes... */
    public String numberString;

    public int partitionComponentLink;

    public long size;

    /** Zero */
    public long unknown1;

    /** Zero */
    public int unknown2;

    /** Zero */
    public long unknownA;

    /** 00 .. 03 */
    public long unknownB;

    /** 00 00 00 11 */
    public int unknownC;

    /** Zero */
    public int unknownD;

    public UUID volumeGuid;

    protected void doReadFrom(byte[] buffer, int offset) {
        super.doReadFrom(buffer, offset);

        int[] pos = new int[] { offset + 0x18 };

        id = readVarULong(buffer, pos);
        name = readVarString(buffer, pos);
        genString = readVarString(buffer, pos);
        numberString = readVarString(buffer, pos);
        activeString = readString(buffer, 6, pos);
        unknownA = readVarULong(buffer, pos);
        unknownB = readULong(buffer, pos);
        dupCount = readVarULong(buffer, pos);
        unknownC = readUInt(buffer, pos);
        componentCount = readVarULong(buffer, pos);
        unknownD = readUInt(buffer, pos);
        partitionComponentLink = readUInt(buffer, pos);
        unknown1 = readULong(buffer, pos);
        size = readVarLong(buffer, pos);
        unknown2 = readUInt(buffer, pos);
        biosType = readByte(buffer, pos);
        volumeGuid = ByteUtil.readBeUUID(buffer, pos[0]);
        pos[0] += 16;

        if ((flags & 0x0200) != 0) {
            mountHint = readVarString(buffer, pos);
        }
    }
}
