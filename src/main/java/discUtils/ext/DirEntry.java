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

package discUtils.ext;

import java.util.EnumSet;

import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.VfsDirEntry;


public class DirEntry extends VfsDirEntry {

    public DirEntry(DirectoryRecord record) {
        this.record = record;
    }

    @Override public long getCreationTimeUtc() {
        throw new UnsupportedOperationException();
    }

    @Override public EnumSet<FileAttributes> getFileAttributes() {
        throw new UnsupportedOperationException();
    }

    @Override public String getFileName() {
        return getRecord().name;
    }

    @Override public boolean hasVfsFileAttributes() {
        return false;
    }

    @Override public boolean hasVfsTimeInfo() {
        return false;
    }

    @Override public boolean isDirectory() {
        return getRecord().fileType == DirectoryRecord.FileTypeDirectory;
    }

    @Override public boolean isSymlink() {
        return getRecord().fileType == DirectoryRecord.FileTypeSymlink;
    }

    @Override public long getLastAccessTimeUtc() {
        throw new UnsupportedOperationException();
    }

    @Override public long getLastWriteTimeUtc() {
        throw new UnsupportedOperationException();
    }

    private DirectoryRecord record;

    public DirectoryRecord getRecord() {
        return record;
    }

    @Override public long getUniqueCacheId() {
        return getRecord().inode;
    }

    @Override public String toString() {
        return getRecord().name != null ? getRecord().name : "(no name)";
    }
}
