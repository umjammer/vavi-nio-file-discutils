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

import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.StreamUtilities;


public final class BTree<TKey extends BTreeKey<?>> extends InternalBTree {
    private final IBuffer _data;

    private final BTreeHeaderRecord _header;

    private BTreeKeyedNode<TKey> _rootNode;

    public BTree(IBuffer data) {
        _data = data;
        byte[] headerInfo = StreamUtilities.readExact(_data, 0, 114);
        _header = new BTreeHeaderRecord();
        _header.readFrom(headerInfo, 14);
        byte[] node0data = StreamUtilities.readExact(_data, 0, _header.NodeSize);
        BTreeHeaderNode node0 = BTreeNode.readNode(this, node0data, 0) instanceof BTreeHeaderNode ? (BTreeHeaderNode) BTreeNode
                .readNode(this, node0data, 0) : (BTreeHeaderNode) null;
        node0.readFrom(node0data, 0);
        if (node0.getHeaderRecord().RootNode != 0) {
            _rootNode = getKeyedNode(node0.getHeaderRecord().RootNode);
        }

    }

    public int getNodeSize() {
        return _header.NodeSize;
    }

    public byte[] find(TKey key) {
        return _rootNode == null ? null : _rootNode.findKey(key);
    }

    public void visitRange(BTreeVisitor<TKey> visitor) {
        _rootNode.visitRange(visitor);
    }

    public BTreeKeyedNode<TKey> getKeyedNode(int nodeId) {
        byte[] nodeData = StreamUtilities.readExact(_data, nodeId * _header.NodeSize, _header.NodeSize);
        BTreeKeyedNode<TKey> node = BTreeNode.readNode(this, nodeData, 0) instanceof BTreeKeyedNode ? BTreeKeyedNode.class
                .cast(BTreeNode.readNode(this, nodeData, 0)) : (BTreeKeyedNode<TKey>) null;
        node.readFrom(nodeData, 0);
        return node;
    }

}

abstract class InternalBTree {
    abstract int getNodeSize();
}