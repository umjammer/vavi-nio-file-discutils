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

package discUtils.xfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discUtils.core.vfs.IVfsDirectory;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.IOException;


public class Directory extends File implements IVfsDirectory<DirEntry, File> {

    public Directory(Context context, Inode inode) {
        super(context, inode);
    }

    private Map<String, DirEntry> allEntries;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<DirEntry> getAllEntries() {
        if (allEntries != null)
            return new ArrayList<>(allEntries.values());

        Map<String, DirEntry> result = new HashMap<>();
        if (inode.getFormat() == InodeFormat.Local) {
            //shortform directory
            ShortformDirectory sfDir = new ShortformDirectory(context);
            sfDir.readFrom(inode.getDataFork(), 0);
            for (ShortformDirectoryEntry entry : sfDir.getEntries()) {
                result.put(new String(entry.getName(), context.getOptions().getFileNameEncoding()),
                           new DirEntry(entry, context));
            }
        } else if (inode.getFormat() == InodeFormat.Extents) {
            if (inode.getExtents() == 1) {
                BlockDirectory blockDir = new BlockDirectory(context);
                if (context.getSuperBlock().getSbVersion() == 5)
                    blockDir = new BlockDirectoryV5(context);

                IBuffer dirContent = inode.getContentBuffer(context);
                byte[] buffer = StreamUtilities.readAll(dirContent);
                blockDir.readFrom(buffer, 0);
                if (!blockDir.getHasValidMagic())
                    throw new IOException("invalid block directory magic");

                addDirEntries(blockDir.getEntries(), result);
            } else {
                List<Extent> extents = inode.getExtents_();
                addLeafDirExtentEntries(extents, result);
            }
        } else {
            BTreeExtentRoot header = new BTreeExtentRoot();
            header.readFrom(inode.getDataFork(), 0);
            header.loadBtree(context);
            List<Extent> extents = header.getExtents();
            addLeafDirExtentEntries(extents, result);
        }
        allEntries = result;
        return new ArrayList(result.values());
    }

    private void addLeafDirExtentEntries(List<Extent> extents, Map<String, DirEntry> target) {
        long leafOffset = LeafDirectory.LeafOffset / context.getSuperBlock().getBlocksize();
        for (Extent extent : extents) {
            if (extent.getStartOffset() < leafOffset) {
                for (long i = 0; i < extent.getBlockCount(); i++) {
                    byte[] buffer = extent.getData(context,
                                                   i * context.getSuperBlock().getDirBlockSize(),
                                                   context.getSuperBlock().getDirBlockSize());
                    LeafDirectory leafDir = new LeafDirectory(context);
                    if (context.getSuperBlock().getSbVersion() == 5)
                        leafDir = new LeafDirectoryV5(context);

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
            IDirectoryEntry dirEntry = entry instanceof IDirectoryEntry ? (IDirectoryEntry) entry : null;
            if (dirEntry == null)
                continue;

            String name = new String(dirEntry.getName(), context.getOptions().getFileNameEncoding());
            if (name.equals(".") || name.equals(".."))
                continue;

            target.put(name, new DirEntry(dirEntry, context));
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
