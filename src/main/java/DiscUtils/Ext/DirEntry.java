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

package DiscUtils.Ext;

import java.util.EnumSet;

import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Core.Vfs.VfsDirEntry;


public class DirEntry extends VfsDirEntry {
    public DirEntry(DirectoryRecord record) {
        this.record = record;
    }

    public long getCreationTimeUtc() {
        throw new UnsupportedOperationException();
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        throw new UnsupportedOperationException();
    }

    public String getFileName() {
        return getRecord().Name;
    }

    public boolean hasVfsFileAttributes() {
        return false;
    }

    public boolean hasVfsTimeInfo() {
        return false;
    }

    public boolean isDirectory() {
        return getRecord().FileType == DirectoryRecord.FileTypeDirectory;
    }

    public boolean isSymlink() {
        return getRecord().FileType == DirectoryRecord.FileTypeSymlink;
    }

    public long getLastAccessTimeUtc() {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc() {
        throw new UnsupportedOperationException();
    }

    private DirectoryRecord record;

    public DirectoryRecord getRecord() {
        return record;
    }

    public long getUniqueCacheId() {
        return getRecord().Inode;
    }

    public String toString() {
        return getRecord().Name != null ? getRecord().Name : "(no name)";
    }
}
