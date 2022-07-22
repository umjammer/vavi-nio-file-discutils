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


public class TocBlock {

    /** 00 00 08 B6 */
    public int checksum;

    /** Unit? */
    public long item1Size;

    /** Sector Offset from ConfigurationStart */
    public long item1Start;

    /** 'config', length 10 */
    public String item1Str;

    /** Unit? */
    public long item2Size;

    /** Sector Offset from ConfigurationStart */
    public long item2Start;

    /** 'log', length 10 */
    public String item2Str;

    /** 00 .. 01 */
    public long sequenceNumber;

    /** TOCBLOCK */
    public String signature;

    /** 0 */
    public long unknown1;

    /** 00 */
    public long unknown2;

    /** 00 06 00 01 (maybe two values?) */
    public int unknown3;

    /** 00 00 00 00 */
    public int unknown4;

    /** 00 06 00 01 (maybe two values?) */
    public int unknown5;

    /** 00 00 00 00 */
    public int unknown6;

    public void readFrom(byte[] buffer, int offset) {
        signature = EndianUtilities.bytesToString(buffer, offset + 0x00, 8);
        checksum = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x08);
        sequenceNumber = EndianUtilities.toInt64BigEndian(buffer, offset + 0x0C);
        unknown1 = EndianUtilities.toInt64BigEndian(buffer, offset + 0x14);
        unknown2 = EndianUtilities.toInt64BigEndian(buffer, offset + 0x1C);
        item1Str = EndianUtilities.bytesToString(buffer, offset + 0x24, 10).replaceFirst("\0*$", "");
        item1Start = EndianUtilities.toInt64BigEndian(buffer, offset + 0x2E);
        item1Size = EndianUtilities.toInt64BigEndian(buffer, offset + 0x36);
        unknown3 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x3E);
        unknown4 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x42);
        item2Str = EndianUtilities.bytesToString(buffer, offset + 0x46, 10).replaceFirst("\0*$", "");
        item2Start = EndianUtilities.toInt64BigEndian(buffer, offset + 0x50);
        item2Size = EndianUtilities.toInt64BigEndian(buffer, offset + 0x58);
        unknown5 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x60);
        unknown6 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x64);
    }
}
