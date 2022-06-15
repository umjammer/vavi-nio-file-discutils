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

package DiscUtils.SquashFs;

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Core.Vfs.IVfsDirectory;


public class Directory extends File implements IVfsDirectory<DirectoryEntry, File> {
    private final IDirectoryInode _dirInode;

    public Directory(Context context, Inode inode, MetadataRef inodeRef) {
        super(context, inode, inodeRef);
        _dirInode = inode instanceof IDirectoryInode ? (IDirectoryInode) inode : null;
        if (_dirInode == null) {
            throw new IllegalArgumentException("Inode is not a directory");
        }
    }

    public List<DirectoryEntry> getAllEntries() {
        List<DirectoryEntry> records = new ArrayList<>();
        MetablockReader reader = getContext().getDirectoryReader();
        reader.setPosition(_dirInode.getStartBlock(), _dirInode.getOffset());
        while (reader.distanceFrom(_dirInode.getStartBlock(), _dirInode.getOffset()) < _dirInode.getFileSize() - 3) {
            // For some reason, always 3 greater than actual..
            DirectoryHeader header = DirectoryHeader.readFrom(reader);
            for (int i = 0; i < header.Count + 1; ++i) {
                DirectoryRecord record = DirectoryRecord.readFrom(reader);
                records.add(new DirectoryEntry(header, record));
            }
        }
        return records;
    }

    public DirectoryEntry getSelf() {
        return null;
    }

    public DirectoryEntry getEntryByName(String name) {
        for (DirectoryEntry entry : getAllEntries()) {
            if (entry.getFileName().equals(name)) {
                return entry;
            }
        }
        return null;
    }

    public DirectoryEntry createNewFile(String name) {
        throw new UnsupportedOperationException();
    }
}
