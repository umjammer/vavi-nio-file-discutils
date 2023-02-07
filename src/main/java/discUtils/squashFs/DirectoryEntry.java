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

package discUtils.squashFs;

import java.util.EnumSet;

import discUtils.core.UnixFileType;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.VfsDirEntry;


public class DirectoryEntry extends VfsDirEntry {

    private final DirectoryHeader header;

    private final DirectoryRecord record;

    public DirectoryEntry(DirectoryHeader header, DirectoryRecord record) {
        this.header = header;
        this.record = record;
    }

    @Override public long getCreationTimeUtc() {
        throw new UnsupportedOperationException();
    }

    @Override public EnumSet<FileAttributes> getFileAttributes() {
        UnixFileType fileType = VfsSquashFileSystemReader.fileTypeFromInodeType(record.type);
        return UnixFileType.toFileAttributes(fileType);
    }

    @Override public String getFileName() {
        return record.name;
    }

    @Override public boolean hasVfsFileAttributes() {
        return true;
    }

    @Override public boolean hasVfsTimeInfo() {
        return false;
    }

    public MetadataRef getInodeReference() {
        return new MetadataRef(header.startBlock, record.getOffset());
    }

    @Override public boolean isDirectory() {
        return record.type == InodeType.Directory || record.type == InodeType.ExtendedDirectory;
    }

    @Override public boolean isSymlink() {
        return record.type == InodeType.Symlink || record.type == InodeType.ExtendedSymlink;
    }

    @Override public long getLastAccessTimeUtc() {
        throw new UnsupportedOperationException();
    }

    @Override public long getLastWriteTimeUtc() {
        throw new UnsupportedOperationException();
    }

    @Override public long getUniqueCacheId() {
        return header.inodeNumber + record.inodeNumber;
    }
}
