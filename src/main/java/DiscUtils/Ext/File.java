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

import java.time.Instant;
import java.util.EnumSet;

import DiscUtils.Core.UnixFileType;
import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Core.Vfs.IVfsFile;
import DiscUtils.Streams.Buffer.IBuffer;


public class File implements IVfsFile {
    private IBuffer _content;

    public File(Context context, int inodeNum, Inode inode) {
        _context = context;
        _inodeNumber = inodeNum;
        _inode = inode;
    }

    private Context _context;

    protected Context getContext() {
        return _context;
    }

    private Inode _inode;

    public Inode getInode() {
        return _inode;
    }

    private int _inodeNumber;

    public int getInodeNumber() {
        return _inodeNumber;
    }

    public long getLastAccessTimeUtc() {
        return Instant.ofEpochSecond(getInode().AccessTime).toEpochMilli();
    }

    public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc() {
        return Instant.ofEpochSecond(getInode().ModificationTime).toEpochMilli();
    }

    public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getCreationTimeUtc() {
        return Instant.ofEpochSecond(getInode().CreationTime).toEpochMilli();
    }

    public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        return fromMode(getInode().Mode);
    }

    public void setFileAttributes(EnumSet<FileAttributes> value) {
        throw new UnsupportedOperationException();
    }

    public long getFileLength() {
        return getInode().FileSize;
    }

    public IBuffer getFileContent() {
        if (_content == null) {
            _content = getInode().getContentBuffer(getContext());
        }

        return _content;
    }

    private static EnumSet<FileAttributes> fromMode(int mode) {
        return UnixFileType.toFileAttributes(UnixFileType.values()[(mode >>> 12) & 0xF]);
    }
}
