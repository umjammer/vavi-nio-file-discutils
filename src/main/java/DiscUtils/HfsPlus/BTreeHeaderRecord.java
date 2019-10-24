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

package DiscUtils.HfsPlus;

import DiscUtils.Streams.Util.EndianUtilities;


public class BTreeHeaderRecord extends BTreeNodeRecord {
    public int Attributes;

    public int ClumpSize;

    public int FirstLeafNode;

    public int FreeNodes;

    public byte KeyCompareType;

    public int LastLeafNode;

    public short MaxKeyLength;

    public short NodeSize;

    public int NumLeafRecords;

    public short Res1;

    public int RootNode;

    public int TotalNodes;

    public short TreeDepth;

    public byte TreeType;

    public int sizeOf() {
        return 104;
    }

    public int readFrom(byte[] buffer, int offset) {
        TreeDepth = EndianUtilities.toUInt16BigEndian(buffer, offset + 0);
        RootNode = EndianUtilities.toUInt32BigEndian(buffer, offset + 2);
        NumLeafRecords = EndianUtilities.toUInt32BigEndian(buffer, offset + 6);
        FirstLeafNode = EndianUtilities.toUInt32BigEndian(buffer, offset + 10);
        LastLeafNode = EndianUtilities.toUInt32BigEndian(buffer, offset + 14);
        NodeSize = EndianUtilities.toUInt16BigEndian(buffer, offset + 18);
        MaxKeyLength = EndianUtilities.toUInt16BigEndian(buffer, offset + 20);
        TotalNodes = EndianUtilities.toUInt16BigEndian(buffer, offset + 22);
        FreeNodes = EndianUtilities.toUInt32BigEndian(buffer, offset + 24);
        Res1 = EndianUtilities.toUInt16BigEndian(buffer, offset + 28);
        ClumpSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 30);
        TreeType = buffer[offset + 34];
        KeyCompareType = buffer[offset + 35];
        Attributes = EndianUtilities.toUInt32BigEndian(buffer, offset + 36);
        return 104;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
