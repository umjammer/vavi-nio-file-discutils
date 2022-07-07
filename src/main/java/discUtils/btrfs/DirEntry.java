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
    private InodeItem _inode;

    private DirIndex _item;

    private final long _treeId;

    public DirEntry(long treeId, long objectId) {
        _treeId = treeId;
        _objectId = objectId;
    }

    public DirEntry(long treeId, DirIndex item, InodeItem inode) {
        this(treeId, item.getChildLocation().getObjectId());
        _inode = inode;
        _item = item;
    }

    public long getCreationTimeUtc() {
        return _inode.getCTime().getDateTime();
    }

    public long getLastAccessTimeUtc() {
        return _inode.getATime().getDateTime();
    }

    public long getLastWriteTimeUtc() {
        return _inode.getMTime().getDateTime();
    }

    public boolean hasVfsTimeInfo() {
        return true;
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        UnixFileType unixFileType = UnixFileType.None;
        switch (_item.getChildType()) {
        case Unknown:
            unixFileType = UnixFileType.None;
            break;
        case RegularFile:
            unixFileType = UnixFileType.Regular;
            break;
        case Directory:
            unixFileType = UnixFileType.Directory;
            break;
        case CharDevice:
            unixFileType = UnixFileType.Character;
            break;
        case BlockDevice:
            unixFileType = UnixFileType.Block;
            break;
        case Fifo:
            unixFileType = UnixFileType.Fifo;
            break;
        case Socket:
            unixFileType = UnixFileType.Socket;
            break;
        case Symlink:
            unixFileType = UnixFileType.Link;
            break;
        case ExtendedAttribute:
            unixFileType = UnixFileType.None;
            break;
        }
        EnumSet<FileAttributes> result = UnixFileType.toFileAttributes(unixFileType);
        if (_inode != null && _inode.getFlags().contains(InodeFlag.Readonly))
            result.add(FileAttributes.ReadOnly);

        return result;
    }

    public boolean hasVfsFileAttributes() {
        return _item != null;
    }

    public String getFileName() {
        return _item.getName();
    }

    public boolean isDirectory() {
        return _item.getChildType() == DirItemChildType.Directory;
    }

    public boolean isSymlink() {
        return _item.getChildType() == DirItemChildType.Symlink;
    }

    public long getUniqueCacheId() {
        long result = _inode == null ? 0 : _inode.getTransId();
        result = (result * 397) ^ _item.getTransId();
        result = (result * 397) ^ _item.getChildLocation().getObjectId();
        return result;
    }

    private Directory _cachedDirectory;

    public Directory getCachedDirectory() {
        return _cachedDirectory;
    }

    public void setCachedDirectory(Directory value) {
        _cachedDirectory = value;
    }

    public DirItemChildType getType() {
        return _item.getChildType();
    }

    private long _objectId;

    public long getObjectId() {
        return _objectId;
    }

    public void setObjectId(long value) {
        _objectId = value;
    }

    public long getTreeId() {
        return _treeId;
    }

    public long getFileSize() {
        return _inode.getFileSize();
    }

    public boolean isSubtree() {
        return _item != null && _item.getChildLocation().getItemType() == ItemType.RootItem;
    }
}
