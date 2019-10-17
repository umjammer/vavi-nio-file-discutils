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


public final class BTreeLeafNode<TKey extends BTreeKey<?>> extends BTreeKeyedNode<TKey> {
    private List<BTreeLeafRecord<TKey>> _records;

    public BTreeLeafNode(Class<TKey> clazz, BTree<?> tree, BTreeNodeDescriptor descriptor) {
        super(clazz, tree, descriptor);
    }

    public byte[] findKey(TKey key) {
        int idx = 0;
        while (idx < _records.size()) {
            int compResult = key.compareTo(_records.get(idx).getKey());
            if (compResult == 0) {
                return _records.get(idx).getData();
            }

            if (compResult < 0) {
                return null;
            }

            ++idx;
        }
        return null;
    }

    public void visitRange(BTreeVisitor<TKey> visitor) {
        int idx = 0;
        while (idx < _records.size() && visitor.invoke(_records.get(idx).getKey(), _records.get(idx).getData()) <= 0) {
            idx++;
        }
    }

    protected List<BTreeNodeRecord> readRecords(byte[] buffer, int offset) {
        int numRecords = getDescriptor().NumRecords;
        int nodeSize = getTree().getNodeSize();
        _records = new ArrayList<>(numRecords);
        int start = EndianUtilities.toUInt16BigEndian(buffer, offset + nodeSize - 2);
        for (int i = 0; i < numRecords; ++i) {
            int end = EndianUtilities.toUInt16BigEndian(buffer, offset + nodeSize - (i + 2) * 2);
            _records.add(i, new BTreeLeafRecord<>(keyClass, end - start));
            _records.get(i).readFrom(buffer, offset + start);
            start = end;
        }
        return List.class.cast(_records);
    }
}
