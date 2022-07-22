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

package discUtils.hfsPlus;

import discUtils.streams.util.EndianUtilities;


class BTreeHeaderRecord extends BTreeNodeRecord {

    public int attributes;

    public int clumpSize;

    public int firstLeafNode;

    public int freeNodes;

    public byte keyCompareType;

    public int lastLeafNode;

    public short maxKeyLength;

    private short nodeSize;

    public int getNodeSize() {
        return nodeSize & 0xffff;
    }

    public int numLeafRecords;

    public short res1;

    public int rootNode;

    public int totalNodes;

    public short treeDepth;

    public byte treeType;

    public int size() {
        return 104;
    }

    public int readFrom(byte[] buffer, int offset) {
        treeDepth = EndianUtilities.toUInt16BigEndian(buffer, offset + 0);
        rootNode = EndianUtilities.toUInt32BigEndian(buffer, offset + 2);
        numLeafRecords = EndianUtilities.toUInt32BigEndian(buffer, offset + 6);
        firstLeafNode = EndianUtilities.toUInt32BigEndian(buffer, offset + 10);
        lastLeafNode = EndianUtilities.toUInt32BigEndian(buffer, offset + 14);
        nodeSize = EndianUtilities.toUInt16BigEndian(buffer, offset + 18);
        maxKeyLength = EndianUtilities.toUInt16BigEndian(buffer, offset + 20);
        totalNodes = EndianUtilities.toUInt16BigEndian(buffer, offset + 22);
        freeNodes = EndianUtilities.toUInt32BigEndian(buffer, offset + 24);
        res1 = EndianUtilities.toUInt16BigEndian(buffer, offset + 28);
        clumpSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 30);
        treeType = buffer[offset + 34];
        keyCompareType = buffer[offset + 35];
        attributes = EndianUtilities.toUInt32BigEndian(buffer, offset + 36);

        return 104;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
