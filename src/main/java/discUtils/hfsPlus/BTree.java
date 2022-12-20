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

import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.StreamUtilities;


final class BTree<TKey extends BTreeKey<?>> extends InternalBTree {

    private Class<TKey> keyClass;

    private final IBuffer data;

    private final BTreeHeaderRecord header;

    private BTreeKeyedNode<TKey> rootNode;

    public BTree(Class<TKey> clazz, IBuffer data) {
        keyClass = clazz;

        this.data = data;

        byte[] headerInfo = StreamUtilities.readExact(this.data, 0, 114);

        header = new BTreeHeaderRecord();
        header.readFrom(headerInfo, 14);

        byte[] node0data = StreamUtilities.readExact(this.data, 0, header.getNodeSize());

        BTreeNode<?> node = BTreeNode.readNode(keyClass, this, node0data, 0);
        @SuppressWarnings("unchecked")
        BTreeHeaderNode<TKey> node0 = (BTreeHeaderNode<TKey>) node;
        node0.readFrom(node0data, 0);

        if (node0.getHeaderRecord().rootNode != 0) {
            rootNode = getKeyedNode(node0.getHeaderRecord().rootNode);
        }
    }

    int getNodeSize() {
        return header.getNodeSize();
    }

    public byte[] find(TKey key) {
        return rootNode == null ? null : rootNode.findKey(key);
    }

    public void visitRange(BTreeVisitor<TKey> visitor) {
        rootNode.visitRange(visitor);
    }

    BTreeKeyedNode<TKey> getKeyedNode(int nodeId) {
        byte[] nodeData = StreamUtilities.readExact(data, (long) nodeId * header.getNodeSize(), header.getNodeSize());

        BTreeNode<TKey> node_ = BTreeNode.readNode2(keyClass, this, nodeData, 0);
        BTreeKeyedNode<TKey> node = (BTreeKeyedNode<TKey>) node_;
        node.readFrom(nodeData, 0);
        return node;
    }
}

abstract class InternalBTree {
    abstract int getNodeSize();
}
