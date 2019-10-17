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

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Streams.Util.EndianUtilities;


public class BTreeHeaderNode<TKey extends BTreeKey<?>> extends BTreeNode<TKey> {
    public BTreeHeaderNode(Class<TKey> clazz, BTree<?> tree, BTreeNodeDescriptor descriptor) {
        super(clazz, tree, descriptor);
    }

    public BTreeHeaderRecord getHeaderRecord() {
        return getRecords().get(0) instanceof BTreeHeaderRecord ? BTreeHeaderRecord.class.cast(getRecords().get(0)) : null;
    }

    protected List<BTreeNodeRecord> readRecords(byte[] buffer, int offset) {
        int totalRecords = getDescriptor().NumRecords;
        int nodeSize = getTree().getNodeSize();
        int headerRecordOffset = EndianUtilities.toUInt16BigEndian(buffer, nodeSize - 2);
        int userDataRecordOffset = EndianUtilities.toUInt16BigEndian(buffer, nodeSize - 4);
        int mapRecordOffset = EndianUtilities.toUInt16BigEndian(buffer, nodeSize - 6);
        List<BTreeNodeRecord> results = new ArrayList<>(3);
        results.add(0, new BTreeHeaderRecord());
        results.get(0).readFrom(buffer, offset + headerRecordOffset);
        results.add(1, new BTreeGenericRecord(mapRecordOffset - userDataRecordOffset));
        results.get(1).readFrom(buffer, offset + userDataRecordOffset);
        results.add(2, new BTreeGenericRecord(nodeSize - (totalRecords * 2 + mapRecordOffset)));
        results.get(2).readFrom(buffer, offset + mapRecordOffset);
        return results;
    }
}
