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

    private final IBuffer _data;

    private final BTreeHeaderRecord _header;

    private BTreeKeyedNode<TKey> _rootNode;

    public BTree(Class<TKey> clazz, IBuffer data) {
        keyClass = clazz;

        _data = data;

        byte[] headerInfo = StreamUtilities.readExact(_data, 0, 114);

        _header = new BTreeHeaderRecord();
        _header.readFrom(headerInfo, 14);

        byte[] node0data = StreamUtilities.readExact(_data, 0, _header.getNodeSize());

        BTreeNode<?> node = BTreeNode.readNode(keyClass, this, node0data, 0);
        BTreeHeaderNode<TKey> node0 = (BTreeHeaderNode) node;
        node0.readFrom(node0data, 0);

        if (node0.getHeaderRecord().RootNode != 0) {
            _rootNode = getKeyedNode(node0.getHeaderRecord().RootNode);
        }
    }

    int getNodeSize() {
        return _header.getNodeSize();
    }

    public byte[] find(TKey key) {
        return _rootNode == null ? null : _rootNode.findKey(key);
    }

    public void visitRange(BTreeVisitor<TKey> visitor) {
        _rootNode.visitRange(visitor);
    }

    BTreeKeyedNode<TKey> getKeyedNode(int nodeId) {
        byte[] nodeData = StreamUtilities.readExact(_data, (long) nodeId * _header.getNodeSize(), _header.getNodeSize());

        BTreeNode<TKey> node_ = BTreeNode.readNode2(keyClass, this, nodeData, 0);
        BTreeKeyedNode<TKey> node = (BTreeKeyedNode) node_;
        node.readFrom(nodeData, 0);
        return node;
    }
}

abstract class InternalBTree {
    abstract int getNodeSize();
}
