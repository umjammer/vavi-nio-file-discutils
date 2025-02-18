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

import java.util.List;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


abstract class BTreeNode<TKey extends BTreeKey<?>> implements IByteArraySerializable {

    protected Class<TKey> keyClass;

    public BTreeNode(Class<TKey> clazz, BTree<?> tree, BTreeNodeDescriptor descriptor) {
        keyClass = clazz;
        this.tree = tree;
        this.descriptor = descriptor;
    }

    private BTreeNodeDescriptor descriptor;

    protected BTreeNodeDescriptor getDescriptor() {
        return descriptor;
    }

    private List<BTreeNodeRecord> records;

    public List<BTreeNodeRecord> getRecords() {
        return records;
    }

    public void setRecords(List<BTreeNodeRecord> value) {
        records = value;
    }

    private BTree<?> tree;

    protected BTree<?> getTree() {
        return tree;
    }

    @Override public int size() {
        return getTree().getNodeSize();
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        setRecords(readRecords(buffer, offset));

        return 0;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public static <TKey extends BTreeKey<?>> BTreeNode<?> readNode(Class<TKey> clazz, BTree<?> tree, byte[] buffer, int offset) {
        BTreeNodeDescriptor descriptor = EndianUtilities
                .toStruct(BTreeNodeDescriptor.class, buffer, offset);

        return switch (descriptor.kind) {
            case HeaderNode -> new BTreeHeaderNode<>(clazz, tree, descriptor);
            case IndexNode, LeafNode ->
                    throw new UnsupportedOperationException("Attempt to read index/leaf node without key and data types");
            default -> throw new UnsupportedOperationException("Unrecognized BTree node kind: " + descriptor.kind);
        };
    }

    public static <TKey extends BTreeKey<?>> BTreeNode<TKey> readNode2(Class<TKey> clazz, BTree<?> tree, byte[] buffer, int offset) {
        BTreeNodeDescriptor descriptor = EndianUtilities
                .toStruct(BTreeNodeDescriptor.class, buffer, offset);

        return switch (descriptor.kind) {
            case HeaderNode -> new BTreeHeaderNode<>(clazz, tree, descriptor);
            case LeafNode -> new BTreeLeafNode<>(clazz, tree, descriptor);
            case IndexNode -> new BTreeIndexNode<>(clazz, tree, descriptor);
            default -> throw new UnsupportedOperationException("Unrecognized BTree node kind: " + descriptor.kind);
        };
    }

    protected List<BTreeNodeRecord> readRecords(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
