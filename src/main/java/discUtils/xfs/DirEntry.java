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

import java.util.EnumSet;

import discUtils.core.UnixFileType;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.VfsDirEntry;


public class DirEntry extends VfsDirEntry {

    private IDirectoryEntry entry;

    private final Context context;

    private String name;

    private Directory cachedDirectory;

    public Directory getCachedDirectory() {
        return cachedDirectory;
    }

    public void setCachedDirectory(Directory value) {
        cachedDirectory = value;
    }

    private DirEntry(Context context) {
        this.context = context;
    }

    public DirEntry(IDirectoryEntry entry, Context context) {
        this(context);
        this.entry = entry;
        name = new String(this.entry.getName(), this.context.getOptions().getFileNameEncoding());
        setInode(this.context.getInode(this.entry.getInode()));
    }

    private Inode inode;

    public Inode getInode() {
        return inode;
    }

    public void setInode(Inode value) {
        inode = value;
    }

    public boolean isDirectory() {
        return inode.getFileType() == UnixFileType.Directory;
    }

    public boolean isSymlink() {
        return inode.getFileType() == UnixFileType.Link;
    }

    public String getFileName() {
        return name;
    }

    public boolean hasVfsTimeInfo() {
        return true;
    }

    public long getLastAccessTimeUtc() {
        return inode.getAccessTime();
    }

    public long getLastWriteTimeUtc() {
        return inode.getModificationTime();
    }

    public long getCreationTimeUtc() {
        return inode.getCreationTime();
    }

    public boolean hasVfsFileAttributes() {
        return true;
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        return UnixFileType.toFileAttributes(inode.getFileType());
    }

    public long getUniqueCacheId() {
        return ((long) inode.getAllocationGroup()) << 32 | inode.getRelativeInodeNumber();
    }
}
