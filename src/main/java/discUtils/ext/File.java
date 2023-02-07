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

import java.time.Instant;
import java.util.EnumSet;

import discUtils.core.UnixFileType;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.IVfsFile;
import discUtils.streams.buffer.IBuffer;


public class File implements IVfsFile {

    private IBuffer content;

    public File(Context context, int inodeNum, Inode inode) {
        this.context = context;
        inodeNumber = inodeNum;
        this.inode = inode;
    }

    private Context context;

    protected Context getContext() {
        return context;
    }

    private Inode inode;

    public Inode getInode() {
        return inode;
    }

    private int inodeNumber;

    public int getInodeNumber() {
        return inodeNumber;
    }

    @Override public long getLastAccessTimeUtc() {
        return Instant.ofEpochSecond(inode.accessTime).toEpochMilli();
    }

    @Override public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public long getLastWriteTimeUtc() {
        return Instant.ofEpochSecond(inode.modificationTime).toEpochMilli();
    }

    @Override public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public long getCreationTimeUtc() {
        return Instant.ofEpochSecond(inode.creationTime).toEpochMilli();
    }

    @Override public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public EnumSet<FileAttributes> getFileAttributes() {
        return fromMode(inode.mode);
    }

    @Override public void setFileAttributes(EnumSet<FileAttributes> value) {
        throw new UnsupportedOperationException();
    }

    @Override public long getFileLength() {
        return inode.fileSize;
    }

    @Override public IBuffer getFileContent() {
        if (content == null) {
            content = getInode().getContentBuffer(getContext());
        }

        return content;
    }

    private static EnumSet<FileAttributes> fromMode(int mode) {
        return UnixFileType.toFileAttributes(UnixFileType.values()[(mode >>> 12) & 0xF]);
    }
}
