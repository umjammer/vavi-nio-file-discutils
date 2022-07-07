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
import discUtils.core.vfs.IVfsFile;
import discUtils.streams.buffer.IBuffer;


public class File implements IVfsFile {
    private FileContentBuffer _content;

    private final MetadataRef _inodeRef;

    public File(Context context, Inode inode, MetadataRef inodeRef) {
        __Context = context;
        __Inode = inode;
        _inodeRef = inodeRef;
    }

    private Context __Context;

    protected Context getContext() {
        return __Context;
    }

    private Inode __Inode;

    public Inode getInode() {
        return __Inode;
    }

    public long getLastAccessTimeUtc() {
        return getInode()._modificationTime;
    }

    public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc() {
        return getInode()._modificationTime;
    }

    public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getCreationTimeUtc() {
        return getInode()._modificationTime;
    }

    public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        UnixFileType fileType = VfsSquashFileSystemReader.fileTypeFromInodeType(getInode()._type);
        return UnixFileType.toFileAttributes(fileType);
    }

    public void setFileAttributes(EnumSet<FileAttributes> value) {
        throw new UnsupportedOperationException();
    }

    public long getFileLength() {
        return getInode().getFileSize();
    }

    public IBuffer getFileContent() {
        if (_content == null) {
            _content = new FileContentBuffer(getContext(), (RegularInode) getInode(), _inodeRef);
        }

        return _content;
    }
}
