//
// Copyright (c) 2008-2011, Kenneth Bell
// Copyright (c) 2016, Bianco Veigel
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

package DiscUtils.Xfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Core.Vfs.IVfsDirectory;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.IOException;


public class Directory extends File implements IVfsDirectory<DirEntry, File> {
    public Directory(Context context, Inode inode) {
        super(context, inode);
    }

    private Map<String, DirEntry> _allEntries;

    public List<DirEntry> getAllEntries() {
        if (_allEntries != null)
            return new ArrayList<>(_allEntries.values());

        Map<String, DirEntry> result = new HashMap<>();
        if (Inode.getFormat() == InodeFormat.Local) {
            //shortform directory
            ShortformDirectory sfDir = new ShortformDirectory(Context);
            sfDir.readFrom(Inode.getDataFork(), 0);
            for (ShortformDirectoryEntry entry : sfDir.getEntries()) {
                result.put(new String(entry.getName(), Context.getOptions().getFileNameEncoding()),
                           new DirEntry(entry, Context));
            }
        } else if (Inode.getFormat() == InodeFormat.Extents) {
            if (Inode.getExtents() == 1) {
                BlockDirectory blockDir = new BlockDirectory(Context);
                if (Context.getSuperBlock().getSbVersion() == 5)
                    blockDir = new BlockDirectoryV5(Context);

                IBuffer dirContent = Inode.getContentBuffer(Context);
                byte[] buffer = StreamUtilities.readAll(dirContent);
                blockDir.readFrom(buffer, 0);
                if (!blockDir.getHasValidMagic())
                    throw new IOException("invalid block directory magic");

                addDirEntries(blockDir.getEntries(), result);
            } else {
                List<Extent> extents = Inode.getExtents_();
                addLeafDirExtentEntries(extents, result);
            }
        } else {
            BTreeExtentRoot header = new BTreeExtentRoot();
            header.readFrom(Inode.getDataFork(), 0);
            header.loadBtree(Context);
            List<Extent> extents = header.getExtents();
            addLeafDirExtentEntries(extents, result);
        }
        _allEntries = result;
        return new ArrayList(result.values());
    }

    private void addLeafDirExtentEntries(List<Extent> extents, Map<String, DirEntry> target) {
        long leafOffset = LeafDirectory.LeafOffset / Context.getSuperBlock().getBlocksize();
        for (Extent extent : extents) {
            if (extent.getStartOffset() < leafOffset) {
                for (long i = 0; i < extent.getBlockCount(); i++) {
                    byte[] buffer = extent.getData(Context,
                                                   i * Context.getSuperBlock().getDirBlockSize(),
                                                   Context.getSuperBlock().getDirBlockSize());
                    LeafDirectory leafDir = new LeafDirectory(Context);
                    if (Context.getSuperBlock().getSbVersion() == 5)
                        leafDir = new LeafDirectoryV5(Context);

                    leafDir.readFrom(buffer, 0);
                    if (!leafDir.getHasValidMagic())
                        throw new IOException("invalid leaf directory magic");

                    addDirEntries(leafDir.getEntries(), target);
                }
            }
        }
    }

    private void addDirEntries(List<BlockDirectoryData> entries, Map<String, DirEntry> target) {
        for (BlockDirectoryData entry : entries) {
            IDirectoryEntry dirEntry = entry instanceof IDirectoryEntry ? (IDirectoryEntry) entry : (IDirectoryEntry) null;
            if (dirEntry == null)
                continue;

            String name = new String(dirEntry.getName(), Context.getOptions().getFileNameEncoding());
            if (name.equals(".") || name.equals(".."))
                continue;

            target.put(name, new DirEntry(dirEntry, Context));
        }
    }

    public DirEntry getSelf() {
        return null;
    }

    public DirEntry getEntryByName(String name) {
        for (DirEntry entry  : getAllEntries()) {
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
