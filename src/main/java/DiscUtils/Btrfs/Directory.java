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

package DiscUtils.Btrfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Btrfs.Base.ItemType;
import DiscUtils.Btrfs.Base.Key;
import DiscUtils.Btrfs.Base.NodeHeader;
import DiscUtils.Btrfs.Base.Items.BaseItem;
import DiscUtils.Btrfs.Base.Items.DirIndex;
import DiscUtils.Btrfs.Base.Items.InodeItem;
import DiscUtils.Btrfs.Base.Items.RootItem;
import DiscUtils.Core.Vfs.IVfsDirectory;


public class Directory extends File implements IVfsDirectory<DirEntry, File> {
    public Directory(DirEntry dirEntry, Context context) {
        super(dirEntry, context);
    }

    private Map<String, DirEntry> _allEntries;

    public List<DirEntry> getAllEntries() {
        if (_allEntries != null)
            return new ArrayList(_allEntries.values());

        Map<String, DirEntry> result = new HashMap<>();
        long treeId = DirEntry.getTreeId();
        long objectId = DirEntry.getObjectId();
        if (DirEntry.getIsSubtree()) {
            treeId = objectId;
            RootItem rootItem = Context.getRootTreeRoot()
                    .findFirst(RootItem.class, new Key(treeId, ItemType.RootItem), Context);
            objectId = rootItem.getRootDirId();
        }

        NodeHeader tree = Context.getFsTree(treeId);
        List<DirIndex> items = tree.find(DirIndex.class, new Key(objectId, ItemType.DirIndex), Context);
        for (DirIndex item : items) {
            BaseItem inode = tree.findFirst(item.getChildLocation(), Context);
            result.put(item.getName(), new DirEntry(treeId, item, (InodeItem) inode));
        }
        _allEntries = result;
        return new ArrayList(result.values());
    }

    public DirEntry getSelf() {
        return DirEntry;
    }

    public DirEntry getEntryByName(String name) {
        for (DirEntry entry : getAllEntries()) {
            if (entry.getFileName().equals(name)) {
                return entry;
            }
        }
        return null;
    }

    public DirEntry createNewFile(String name) {
        throw new UnsupportedOperationException();
    }
}
