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

package DiscUtils.Btrfs.Base;

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Btrfs.Context;
import DiscUtils.Btrfs.Base.Items.BaseItem;
import DiscUtils.Btrfs.Base.Items.ChunkItem;
import DiscUtils.Btrfs.Base.Items.DevItem;
import DiscUtils.Btrfs.Base.Items.DirIndex;
import DiscUtils.Btrfs.Base.Items.DirItem;
import DiscUtils.Btrfs.Base.Items.ExtentData;
import DiscUtils.Btrfs.Base.Items.InodeItem;
import DiscUtils.Btrfs.Base.Items.InodeRef;
import DiscUtils.Btrfs.Base.Items.OrphanItem;
import DiscUtils.Btrfs.Base.Items.RootBackref;
import DiscUtils.Btrfs.Base.Items.RootItem;
import DiscUtils.Btrfs.Base.Items.RootRef;
import DiscUtils.Btrfs.Base.Items.XattrItem;
import DiscUtils.Streams.Util.EndianUtilities;
import dotnet4j.io.IOException;


public class LeafNode extends NodeHeader {
    /**
     * key pointers
     */
    private NodeItem[] __Items;

    public NodeItem[] getItems() {
        return __Items;
    }

    public void setItems(NodeItem[] value) {
        __Items = value;
    }

    private BaseItem[] __NodeData;

    public BaseItem[] getNodeData() {
        return __NodeData;
    }

    public void setNodeData(BaseItem[] value) {
        __NodeData = value;
    }

    public int sizeOf() {
        return super.sizeOf() + getItemCount() * KeyPointer.Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        int itemOffset = super.readFrom(buffer, offset);
        setItems(new NodeItem[getItemCount()]);
        setNodeData(new BaseItem[getItemCount()]);
        for (int i = 0; i < getItemCount(); i++) {
            getItems()[i] = new NodeItem();
            itemOffset += getItems()[i].readFrom(buffer, itemOffset);
            long objectId = getItems()[i].getKey().getObjectId();
            if (objectId == ReservedObjectId.CsumItem.ordinal() ||
                objectId == ReservedObjectId.TreeReloc.ordinal()) {
                continue;
            } else {
                getNodeData()[i] = createItem(getItems()[i], buffer, Length + offset);
            }
        }
        return sizeOf();
    }

    private BaseItem createItem(NodeItem item, byte[] buffer, int offset) {
        byte[] data = EndianUtilities.toByteArray(buffer, (offset + item.getDataOffset()), item.getDataSize());
        BaseItem result;
        switch (item.getKey().getItemType()) {
        case ChunkItem:
            result = new ChunkItem(item.getKey());
            break;
        case DevItem:
            result = new DevItem(item.getKey());
            break;
        case RootItem:
            result = new RootItem(item.getKey());
            break;
        case InodeRef:
            result = new InodeRef(item.getKey());
            break;
        case InodeItem:
            result = new InodeItem(item.getKey());
            break;
        case DirItem:
            result = new DirItem(item.getKey());
            break;
        case DirIndex:
            result = new DirIndex(item.getKey());
            break;
        case ExtentData:
            result = new ExtentData(item.getKey());
            break;
        case RootRef:
            result = new RootRef(item.getKey());
            break;
        case RootBackref:
            result = new RootBackref(item.getKey());
            break;
        case XattrItem:
            result = new XattrItem(item.getKey());
            break;
        case OrphanItem:
            result = new OrphanItem(item.getKey());
            break;
        default:
            throw new IOException("Unsupported item type {item.Key.ItemType}");

        }
        result.readFrom(data, 0);
        return result;
    }

    public List<BaseItem> find(Key key, Context context) {
        List<BaseItem> result = new ArrayList<>();
        for (int i = 0; i < getItems().length; i++) {
            if (getItems()[i].getKey().getObjectId() > key.getObjectId())
                break;

            if (getItems()[i].getKey().getObjectId() == key.getObjectId() &&
                getItems()[i].getKey().getItemType() == key.getItemType())
                result.add(getNodeData()[i]);
        }
        return result;
    }

}
