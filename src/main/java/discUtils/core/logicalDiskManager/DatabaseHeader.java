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

import java.nio.charset.StandardCharsets;

import vavi.util.ByteUtil;


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
        signature = new String(buffer, offset + 0x00, 4, StandardCharsets.US_ASCII);
        numVBlks = ByteUtil.readBeInt(buffer, offset + 0x04);
        blockSize = ByteUtil.readBeInt(buffer, offset + 0x08);
        headerSize = ByteUtil.readBeInt(buffer, offset + 0x0C);
        unknown1 = ByteUtil.readBeShort(buffer, offset + 0x10);
        versionNum = ByteUtil.readBeShort(buffer, offset + 0x12);
        versionDenom = ByteUtil.readBeShort(buffer, offset + 0x14);
        groupName = new String(buffer, offset + 0x16, 31, StandardCharsets.US_ASCII).replaceFirst("\0*$", "");
        diskGroupId = new String(buffer, offset + 0x35, 0x40, StandardCharsets.US_ASCII).replaceFirst("\0*$", "");
        // May be wrong way round...
        committedSequence = ByteUtil.readBeLong(buffer, offset + 0x75);
        pendingSequence = ByteUtil.readBeLong(buffer, offset + 0x7D);
        unknown2 = ByteUtil.readBeInt(buffer, offset + 0x85);
        unknown3 = ByteUtil.readBeInt(buffer, offset + 0x89);
        unknown4 = ByteUtil.readBeInt(buffer, offset + 0x8D);
        unknown5 = ByteUtil.readBeInt(buffer, offset + 0x91);
        unknown6 = ByteUtil.readBeLong(buffer, offset + 0x95);
        unknown7 = ByteUtil.readBeLong(buffer, offset + 0x9D);
        unknown8 = ByteUtil.readBeInt(buffer, offset + 0xA5);
        unknown9 = ByteUtil.readBeInt(buffer, offset + 0xA9);
        unknownA = ByteUtil.readBeInt(buffer, offset + 0xAD);
        unknownB = ByteUtil.readBeLong(buffer, offset + 0xB1);
        unknownC = ByteUtil.readBeInt(buffer, offset + 0xB9);
        timestamp = ByteUtil.readBeLong(buffer, offset + 0xBD);
    }
}
