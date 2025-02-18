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

package discUtils.btrfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discUtils.btrfs.base.ItemType;
import discUtils.btrfs.base.Key;
import discUtils.btrfs.base.NodeHeader;
import discUtils.btrfs.base.items.BaseItem;
import discUtils.btrfs.base.items.DirIndex;
import discUtils.btrfs.base.items.InodeItem;
import discUtils.btrfs.base.items.RootItem;
import discUtils.core.vfs.IVfsDirectory;


public class Directory extends File implements IVfsDirectory<DirEntry, File> {

    public Directory(DirEntry dirEntry, Context context) {
        super(dirEntry, context);
    }

    private Map<String, DirEntry> allEntries;

    @Override
    public List<DirEntry> getAllEntries() {
        if (allEntries != null)
            return new ArrayList<>(allEntries.values());

        Map<String, DirEntry> result = new HashMap<>();
        long treeId = dirEntry.getTreeId();
        long objectId = dirEntry.getObjectId();
        if (dirEntry.isSubtree()) {
            treeId = objectId;
            RootItem rootItem = context.getRootTreeRoot()
                    .findFirst(RootItem.class, new Key(treeId, ItemType.RootItem), context);
            objectId = rootItem.getRootDirId();
        }

        NodeHeader tree = context.getFsTree(treeId);
        List<DirIndex> items = tree.find(DirIndex.class, new Key(objectId, ItemType.DirIndex), context);
        for (DirIndex item : items) {
            BaseItem inode = tree.findFirst(item.getChildLocation(), context);
            result.put(item.getName(), new DirEntry(treeId, item, (InodeItem) inode));
        }
        allEntries = result;
        return new ArrayList<>(result.values());
    }

    @Override public DirEntry getSelf() {
        return dirEntry;
    }

    @Override public DirEntry getEntryByName(String name) {
        for (DirEntry entry : getAllEntries()) {
            if (entry.getFileName().equals(name)) {
                return entry;
            }
        }
        return null;
    }

    @Override public DirEntry createNewFile(String name) {
        throw new UnsupportedOperationException();
    }
}
