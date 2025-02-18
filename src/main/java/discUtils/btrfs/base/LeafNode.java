//
// Copyright (c) 2017, Bianco Veigel
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

package discUtils.btrfs.base;

import java.util.ArrayList;
import java.util.List;

import discUtils.btrfs.Context;
import discUtils.btrfs.base.items.BaseItem;
import discUtils.btrfs.base.items.ChunkItem;
import discUtils.btrfs.base.items.DevItem;
import discUtils.btrfs.base.items.DirIndex;
import discUtils.btrfs.base.items.DirItem;
import discUtils.btrfs.base.items.ExtentData;
import discUtils.btrfs.base.items.InodeItem;
import discUtils.btrfs.base.items.InodeRef;
import discUtils.btrfs.base.items.OrphanItem;
import discUtils.btrfs.base.items.RootBackref;
import discUtils.btrfs.base.items.RootItem;
import discUtils.btrfs.base.items.RootRef;
import discUtils.btrfs.base.items.XattrItem;
import discUtils.streams.util.EndianUtilities;
import dotnet4j.io.IOException;


public class LeafNode extends NodeHeader {

    /**
     * key pointers
     */
    private NodeItem[] items;

    public NodeItem[] getItems() {
        return items;
    }

    public void setItems(NodeItem[] value) {
        items = value;
    }

    private BaseItem[] nodeData;

    public BaseItem[] getNodeData() {
        return nodeData;
    }

    public void setNodeData(BaseItem[] value) {
        nodeData = value;
    }

    @Override public int size() {
        return super.size() + getItemCount() * KeyPointer.Length;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        int itemOffset = super.readFrom(buffer, offset);
        items = new NodeItem[getItemCount()];
        nodeData = new BaseItem[getItemCount()];
        for (int i = 0; i < getItemCount(); i++) {
            getItems()[i] = new NodeItem();
            itemOffset += getItems()[i].readFrom(buffer, itemOffset);
            long objectId = getItems()[i].getKey().getObjectId();
            if (objectId == ReservedObjectId.CsumItem.getValue() ||
                objectId == ReservedObjectId.TreeReloc.getValue()) {
            } else {
                getNodeData()[i] = createItem(getItems()[i], buffer, Length + offset);
            }
        }
        return size();
    }

    private BaseItem createItem(NodeItem item, byte[] buffer, int offset) {
        byte[] data = EndianUtilities.toByteArray(buffer, (offset + item.getDataOffset()), item.getDataSize());
        BaseItem result = switch (item.getKey().getItemType()) {
            case ChunkItem -> new ChunkItem(item.getKey());
            case DevItem -> new DevItem(item.getKey());
            case RootItem -> new RootItem(item.getKey());
            case InodeRef -> new InodeRef(item.getKey());
            case InodeItem -> new InodeItem(item.getKey());
            case DirItem -> new DirItem(item.getKey());
            case DirIndex -> new DirIndex(item.getKey());
            case ExtentData -> new ExtentData(item.getKey());
            case RootRef -> new RootRef(item.getKey());
            case RootBackref -> new RootBackref(item.getKey());
            case XattrItem -> new XattrItem(item.getKey());
            case OrphanItem -> new OrphanItem(item.getKey());
            default -> throw new IOException("Unsupported item type {item.Key.ItemType}");
        };
        result.readFrom(data, 0);
        return result;
    }

    @Override public List<BaseItem> find(Key key, Context context) {
        List<BaseItem> result = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            if (items[i].getKey().getObjectId() > key.getObjectId())
                break;

            if (items[i].getKey().getObjectId() == key.getObjectId() &&
                items[i].getKey().getItemType() == key.getItemType())
                result.add(nodeData[i]);
        }
        return result;
    }
}
