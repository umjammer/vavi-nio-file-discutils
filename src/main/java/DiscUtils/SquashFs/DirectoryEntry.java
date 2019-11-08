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

package DiscUtils.SquashFs;

import java.util.EnumSet;

import DiscUtils.Core.UnixFileType;
import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Core.Vfs.VfsDirEntry;


public class DirectoryEntry extends VfsDirEntry {
    private final DirectoryHeader _header;

    private final DirectoryRecord _record;

    public DirectoryEntry(DirectoryHeader header, DirectoryRecord record) {
        _header = header;
        _record = record;
    }

    public long getCreationTimeUtc() {
        throw new UnsupportedOperationException();
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        UnixFileType fileType = VfsSquashFileSystemReader.fileTypeFromInodeType(_record.Type);
        return UnixFileType.toFileAttributes(fileType);
    }

    public String getFileName() {
        return _record.Name;
    }

    public boolean hasVfsFileAttributes() {
        return true;
    }

    public boolean hasVfsTimeInfo() {
        return false;
    }

    public MetadataRef getInodeReference() {
        return new MetadataRef(_header.StartBlock, _record.getOffset());
    }

    public boolean isDirectory() {
        return _record.Type == InodeType.Directory || _record.Type == InodeType.ExtendedDirectory;
    }

    public boolean isSymlink() {
        return _record.Type == InodeType.Symlink || _record.Type == InodeType.ExtendedSymlink;
    }

    public long getLastAccessTimeUtc() {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc() {
        throw new UnsupportedOperationException();
    }

    public long getUniqueCacheId() {
        return _header.InodeNumber + _record.InodeNumber;
    }
}
