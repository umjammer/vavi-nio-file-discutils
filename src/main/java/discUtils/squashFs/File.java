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

    private FileContentBuffer content;

    private final MetadataRef inodeRef;

    public File(Context context, Inode inode, MetadataRef inodeRef) {
        this.context = context;
        this.inode = inode;
        this.inodeRef = inodeRef;
    }

    private final Context context;

    protected Context getContext() {
        return context;
    }

    private final Inode inode;

    public Inode getInode() {
        return inode;
    }

    @Override public long getLastAccessTimeUtc() {
        return getInode().modificationTime;
    }

    @Override public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public long getLastWriteTimeUtc() {
        return getInode().modificationTime;
    }

    @Override public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public long getCreationTimeUtc() {
        return getInode().modificationTime;
    }

    @Override public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public EnumSet<FileAttributes> getFileAttributes() {
        UnixFileType fileType = VfsSquashFileSystemReader.fileTypeFromInodeType(getInode().type);
        return UnixFileType.toFileAttributes(fileType);
    }

    @Override public void setFileAttributes(EnumSet<FileAttributes> value) {
        throw new UnsupportedOperationException();
    }

    @Override public long getFileLength() {
        return getInode().getFileSize();
    }

    @Override public IBuffer getFileContent() {
        if (content == null) {
            content = new FileContentBuffer(context, (RegularInode) inode, inodeRef);
        }

        return content;
    }
}
