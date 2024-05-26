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

import java.util.EnumSet;

import discUtils.btrfs.base.DirItemChildType;
import discUtils.btrfs.base.InodeFlag;
import discUtils.btrfs.base.ItemType;
import discUtils.btrfs.base.items.DirIndex;
import discUtils.btrfs.base.items.InodeItem;
import discUtils.core.UnixFileType;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.VfsDirEntry;


public class DirEntry extends VfsDirEntry {

    private InodeItem inode;

    private DirIndex item;

    private final long treeId;

    public DirEntry(long treeId, long objectId) {
        this.treeId = treeId;
        this.objectId = objectId;
    }

    public DirEntry(long treeId, DirIndex item, InodeItem inode) {
        this(treeId, item.getChildLocation().getObjectId());
        this.inode = inode;
        this.item = item;
    }

    @Override public long getCreationTimeUtc() {
        return inode.getCTime().getDateTime();
    }

    @Override public long getLastAccessTimeUtc() {
        return inode.getATime().getDateTime();
    }

    @Override public long getLastWriteTimeUtc() {
        return inode.getMTime().getDateTime();
    }

    @Override public boolean hasVfsTimeInfo() {
        return true;
    }

    @Override public EnumSet<FileAttributes> getFileAttributes() {
        UnixFileType unixFileType = switch (item.getChildType()) {
            case Unknown -> UnixFileType.None;
            case RegularFile -> UnixFileType.Regular;
            case Directory -> UnixFileType.Directory;
            case CharDevice -> UnixFileType.Character;
            case BlockDevice -> UnixFileType.Block;
            case Fifo -> UnixFileType.Fifo;
            case Socket -> UnixFileType.Socket;
            case Symlink -> UnixFileType.Link;
            case ExtendedAttribute -> UnixFileType.None;
        };
        EnumSet<FileAttributes> result = UnixFileType.toFileAttributes(unixFileType);
        if (inode != null && inode.getFlags().contains(InodeFlag.Readonly))
            result.add(FileAttributes.ReadOnly);

        return result;
    }

    @Override public boolean hasVfsFileAttributes() {
        return item != null;
    }

    @Override public String getFileName() {
        return item.getName();
    }

    @Override public boolean isDirectory() {
        return item.getChildType() == DirItemChildType.Directory;
    }

    @Override public boolean isSymlink() {
        return item.getChildType() == DirItemChildType.Symlink;
    }

    @Override public long getUniqueCacheId() {
        long result = inode == null ? 0 : inode.getTransId();
        result = (result * 397) ^ item.getTransId();
        result = (result * 397) ^ item.getChildLocation().getObjectId();
        return result;
    }

    private Directory cachedDirectory;

    public Directory getCachedDirectory() {
        return cachedDirectory;
    }

    public void setCachedDirectory(Directory value) {
        cachedDirectory = value;
    }

    public DirItemChildType getType() {
        return item.getChildType();
    }

    private long objectId;

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long value) {
        objectId = value;
    }

    public long getTreeId() {
        return treeId;
    }

    public long getFileSize() {
        return inode.getFileSize();
    }

    public boolean isSubtree() {
        return item != null && item.getChildLocation().getItemType() == ItemType.RootItem;
    }
}
