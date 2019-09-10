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

import DiscUtils.Streams.Util.EndianUtilities;


public class DatabaseHeader {
    public int BlockSize;

    // 00 00 00 80
    public long CommittedSequence;

    // 0xA
    public String DiskGroupId;

    public String GroupName;

    public int HeaderSize;

    // 00 00 02 00
    public int NumVBlks;

    // 00 00 17 24
    public long PendingSequence;

    // 0xA
    public String Signature;

    // VMDB
    public long Timestamp;

    public short Unknown1;

    // 00 01
    public int Unknown2;

    // 1
    public int Unknown3;

    // 1
    public int Unknown4;

    // 3
    public int Unknown5;

    // 3
    public long Unknown6;

    // 0
    public long Unknown7;

    // 1
    public int Unknown8;

    // 1
    public int Unknown9;

    // 3
    public int UnknownA;

    // 3
    public long UnknownB;

    // 0
    public int UnknownC;

    // 0
    public short VersionDenom;

    // 00 0a
    public short VersionNum;

    // 00 04
    public void readFrom(byte[] buffer, int offset) {
        Signature = EndianUtilities.bytesToString(buffer, offset + 0x00, 4);
        NumVBlks = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x04);
        BlockSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x08);
        HeaderSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x0C);
        Unknown1 = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x10);
        VersionNum = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x12);
        VersionDenom = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x14);
        GroupName = EndianUtilities.bytesToString(buffer, offset + 0x16, 31).replaceFirst("\0*$", "");
        DiskGroupId = EndianUtilities.bytesToString(buffer, offset + 0x35, 0x40).replaceFirst("\0*$", "");
        // May be wrong way round...
        CommittedSequence = EndianUtilities.toInt64BigEndian(buffer, offset + 0x75);
        PendingSequence = EndianUtilities.toInt64BigEndian(buffer, offset + 0x7D);
        Unknown2 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x85);
        Unknown3 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x89);
        Unknown4 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x8D);
        Unknown5 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x91);
        Unknown6 = EndianUtilities.toInt64BigEndian(buffer, offset + 0x95);
        Unknown7 = EndianUtilities.toInt64BigEndian(buffer, offset + 0x9D);
        Unknown8 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xA5);
        Unknown9 = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xA9);
        UnknownA = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xAD);
        UnknownB = EndianUtilities.toInt64BigEndian(buffer, offset + 0xB1);
        UnknownC = EndianUtilities.toUInt32BigEndian(buffer, offset + 0xB9);
        Timestamp = EndianUtilities.toInt64BigEndian(buffer, offset + 0xBD);
    }

}
