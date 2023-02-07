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

import java.util.ArrayList;
import java.util.List;

import vavi.util.ByteUtil;


class BTreeIndexNode<TKey extends BTreeKey<?>> extends BTreeKeyedNode<TKey> {

    private List<BTreeIndexRecord<TKey>> records;

    public BTreeIndexNode(Class<TKey> clazz, BTree<?> tree, BTreeNodeDescriptor descriptor) {
        super(clazz, tree, descriptor);
    }

    @Override public byte[] findKey(TKey key) {
        int nextResult = records.get(0).getKey().compareTo(key);

        int idx = 0;
        while (idx < records.size()) {
            int thisResult = nextResult;

            if (idx + 1 < records.size()) {
                nextResult = records.get(idx + 1).getKey().compareTo(key);
            } else {
                nextResult = 1;
            }

            if (thisResult > 0) {
                // This record's key is too big, so no chance further records
                // will match.
                return null;
            }
            if (nextResult > 0) {
                // Next record's key is too big, so worth looking at children
                @SuppressWarnings("unchecked")
                BTreeKeyedNode<TKey> child = ((BTree<TKey>) getTree()).getKeyedNode(records.get(idx).getChildId());
                return child.findKey(key);
            }

            idx++;
        }

        return null;
    }

    @Override public void visitRange(BTreeVisitor<TKey> visitor) {
        int nextResult = visitor.invoke(records.get(0).getKey(), null);

        int idx = 0;
        while (idx < records.size()) {
            int thisResult = nextResult;

            if (idx + 1 < records.size()) {
                nextResult = visitor.invoke(records.get(idx + 1).getKey(), null);
            } else {
                nextResult = 1;
            }

            if (thisResult > 0) {
                // This record's key is too big, so no chance further records
                // will match.
                return;
            }
            if (nextResult >= 0) {
                // Next record's key isn't too small, so worth looking at children
                @SuppressWarnings("unchecked")
                BTreeKeyedNode<TKey> child = ((BTree<TKey>) getTree()).getKeyedNode(records.get(idx).getChildId());
                child.visitRange(visitor);
            }

            idx++;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override protected List<BTreeNodeRecord> readRecords(byte[] buffer, int offset) {
        int numRecords = getDescriptor().getNumRecords();
        int nodeSize = getTree().getNodeSize();

        records = new ArrayList<>(numRecords);

        int start = ByteUtil.readBeShort(buffer, offset + nodeSize - 2);

        for (int i = 0; i < numRecords; ++i) {
            int end = ByteUtil.readBeShort(buffer, offset + nodeSize - (i + 2) * 2);

            records.add(i, new BTreeIndexRecord<>(keyClass, end - start));
            records.get(i).readFrom(buffer, offset + start);

            start = end;
        }

        return (List) records;
    }
}
