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

import java.util.Map;

import DiscUtils.Btrfs.Base.DirItemChildType;
import DiscUtils.Btrfs.Base.InodeFlag;
import DiscUtils.Btrfs.Base.ItemType;
import DiscUtils.Btrfs.Base.Items.DirIndex;
import DiscUtils.Btrfs.Base.Items.InodeItem;
import DiscUtils.Core.UnixFileType;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Core.Vfs.VfsDirEntry;


public class DirEntry extends VfsDirEntry {
    private InodeItem _inode;

    private DirIndex _item;

    private final long _treeId;

    public DirEntry(long treeId, long objectId) {
        _treeId = treeId;
        setObjectId(objectId);
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

    public Map<String, Object> getFileAttributes() {
        UnixFileType unixFileType;
        DirItemChildType __dummyScrutVar0 = _item.getChildType();
        if (__dummyScrutVar0.equals(DirItemChildType.Unknown)) {
            unixFileType = UnixFileType.None;
        } else if (__dummyScrutVar0.equals(DirItemChildType.RegularFile)) {
            unixFileType = UnixFileType.Regular;
        } else if (__dummyScrutVar0.equals(DirItemChildType.Directory)) {
            unixFileType = UnixFileType.Directory;
        } else if (__dummyScrutVar0.equals(DirItemChildType.CharDevice)) {
            unixFileType = UnixFileType.Character;
        } else if (__dummyScrutVar0.equals(DirItemChildType.BlockDevice)) {
            unixFileType = UnixFileType.Block;
        } else if (__dummyScrutVar0.equals(DirItemChildType.Fifo)) {
            unixFileType = UnixFileType.Fifo;
        } else if (__dummyScrutVar0.equals(DirItemChildType.Socket)) {
            unixFileType = UnixFileType.Socket;
        } else if (__dummyScrutVar0.equals(DirItemChildType.Symlink)) {
            unixFileType = UnixFileType.Link;
        } else if (__dummyScrutVar0.equals(DirItemChildType.ExtendedAttribute)) {
            unixFileType = UnixFileType.None;
        } else {
            throw new IllegalArgumentException();
        }
        Map<String, Object> result = Utilities.fileAttributesFromUnixFileType(unixFileType);
        if (_inode != null && _inode.getFlags().contains(InodeFlag.Readonly))
            result.put("ReadOnly", true);

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
        long result = _inode == null ? 0 : (long) _inode.getTransId();
        result = (result * 397) ^ _item.getTransId();
        result = (result * 397) ^ _item.getChildLocation().getObjectId();
        return result;
    }

    private Directory __CachedDirectory;

    public Directory getCachedDirectory() {
        return __CachedDirectory;
    }

    public void setCachedDirectory(Directory value) {
        __CachedDirectory = value;
    }

    public DirItemChildType getType() {
        return _item.getChildType();
    }

    private long __ObjectId;

    public long getObjectId() {
        return __ObjectId;
    }

    public void setObjectId(long value) {
        __ObjectId = value;
    }

    public long getTreeId() {
        return _treeId;
    }

    public long getFileSize() {
        return _inode.getFileSize();
    }

    public boolean getIsSubtree() {
        return _item != null && _item.getChildLocation().getItemType() == ItemType.RootItem;
    }
}
