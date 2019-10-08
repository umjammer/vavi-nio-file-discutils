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

import java.util.Map;

import DiscUtils.Core.UnixFileType;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Core.Vfs.VfsDirEntry;


public class DirEntry extends VfsDirEntry {
    private IDirectoryEntry _entry;

    private final Context _context;

    private String _name;

    private Directory __CachedDirectory;

    public Directory getCachedDirectory() {
        return __CachedDirectory;
    }

    public void setCachedDirectory(Directory value) {
        __CachedDirectory = value;
    }

    private DirEntry(Context context) {
        _context = context;
    }

    public DirEntry(IDirectoryEntry entry, Context context) {
        this(context);
        _entry = entry;
        _name = new String(_entry.getName(), _context.getOptions().getFileNameEncoding());
        setInode(_context.getInode(_entry.getInode()));
    }

    private Inode __Inode;

    public Inode getInode() {
        return __Inode;
    }

    public void setInode(Inode value) {
        __Inode = value;
    }

    public boolean isDirectory() {
        return getInode().getFileType() == UnixFileType.Directory;
    }

    public boolean isSymlink() {
        return getInode().getFileType() == UnixFileType.Link;
    }

    public String getFileName() {
        return _name;
    }

    public boolean hasVfsTimeInfo() {
        return true;
    }

    public long getLastAccessTimeUtc() {
        return getInode().getAccessTime();
    }

    public long getLastWriteTimeUtc() {
        return getInode().getModificationTime();
    }

    public long getCreationTimeUtc() {
        return getInode().getCreationTime();
    }

    public boolean hasVfsFileAttributes() {
        return true;
    }

    public Map<String, Object> getFileAttributes() {
        return Utilities.fileAttributesFromUnixFileType(getInode().getFileType());
    }

    public long getUniqueCacheId() {
        return ((long) getInode().getAllocationGroup()) << 32 | getInode().getRelativeInodeNumber();
    }
}
