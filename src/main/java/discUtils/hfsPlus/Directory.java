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

import discUtils.core.vfs.IVfsDirectory;


final class Directory extends File implements IVfsDirectory<DirEntry, File> {

    public Directory(Context context, CatalogNodeId nodeId, CommonCatalogFileInfo fileInfo) {
        super(context, nodeId, fileInfo);
    }

    @Override public List<DirEntry> getAllEntries() {
        List<DirEntry> results = new ArrayList<>();

        getContext().getCatalog().visitRange((key, data) -> {
            if (key.getNodeId().equals(getNodeId())) {
                if (data != null && key.getName() != null && !key.getName().isEmpty() && DirEntry.isFileOrDirectory(data)) {
                    results.add(new DirEntry(key.getName(), data));
                }

                return 0;
            }
            return key.getNodeId().getId() < getNodeId().getId() ? -1 : 1;
        });

        return results;
    }

    @Override public DirEntry getSelf() {
        byte[] dirThreadData = getContext().getCatalog().find(new CatalogKey(getNodeId(), ""));

        CatalogThread dirThread = new CatalogThread();
        dirThread.readFrom(dirThreadData, 0);

        byte[] dirEntryData = getContext().getCatalog().find(new CatalogKey(dirThread.parentId, dirThread.name));

        return new DirEntry(dirThread.name, dirEntryData);
    }

    @Override public DirEntry getEntryByName(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Attempt to lookup empty file name");
        }

        byte[] dirEntryData = getContext().getCatalog().find(new CatalogKey(getNodeId(), name));
        if (dirEntryData == null) {
            return null;
        }

        return new DirEntry(name, dirEntryData);
    }

    @Override public DirEntry createNewFile(String name) {
        throw new UnsupportedOperationException();
    }
}
