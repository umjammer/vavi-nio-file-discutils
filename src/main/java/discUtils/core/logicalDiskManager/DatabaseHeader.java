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

import discUtils.streams.util.EndianUtilities;


public class DatabaseHeader {

    /** 00 00 00 80 */
    public int blockSize;

    /** 0xA */
    public long committedSequence;

    public String diskGroupId;

    public String groupName;

    /** 00 00 02 00 */
    public int headerSize;

    /** 00 00 17 24 */
    public int numVBlks;

    /** 0xA */
    public long pendingSequence;

    /** VMDB */
    public String signature;

    public long timestamp;

    /** 00 01 */
    public short unknown1;

    /** 1 */
    public int unknown2;

    /** 1 */
    public int unknown3;

    /** 3 */
    public int unknown4;

    /** 3 */
    public int unknown5;

    /** 0 */
    public long unknown6;

    /** 1 */
    public long unknown7;

    /** 1 */
    public int unknown8;

    /** 3 */
    public int unknown9;

    /** 3 */
    public int unknownA;

    /** 0 */
    public long unknownB;

    /** 0 */
    public int unknownC;

    /** 00 0a */
    public short versionDenom;

    /** 00 04 */
    public short versionNum;

    public void readFrom(byte[] buffer, int offset) {
        signature = EndianUtilities.bytesToString(buffer, offset + 0x00, 4);
        numVBlks = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x04);
        blockSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x08);
        headerSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x0C);
        unknown1 = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x10);
        versionNum = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x12);
        versionDenom = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x14);
        groupName = EndianUtilities.bytesToString(buffer, offset + 0x16, 31).replaceFirst("\0*$", "");
        diskGroupId = EndianUtilities.bytesToString(buffer, offset + 0x35, 0x40).replaceFirst("\0*$", "");
        // May be wrong way round...
        committedSequence = EndianUtilities.toInt64BigEndian(buffer, offset + 0x75);
        pendingSequence = EndianUtilities.toInt64BigEndian(buffer, offset + 0x7D);
        unknown2 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x85);
        unknown3 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x89);
        unknown4 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x8D);
        unknown5 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x91);
        unknown6 = EndianUtilities.toInt64BigEndian(buffer, offset + 0x95);
        unknown7 = EndianUtilities.toInt64BigEndian(buffer, offset + 0x9D);
        unknown8 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xA5);
        unknown9 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xA9);
        unknownA = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xAD);
        unknownB = EndianUtilities.toInt64BigEndian(buffer, offset + 0xB1);
        unknownC = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xB9);
        timestamp = EndianUtilities.toInt64BigEndian(buffer, offset + 0xBD);
    }
}
