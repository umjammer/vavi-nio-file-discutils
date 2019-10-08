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

import java.util.Map;

import DiscUtils.Core.UnixFileType;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Core.Vfs.IVfsFile;
import DiscUtils.Streams.Buffer.IBuffer;


public class File implements IVfsFile {
    private IBuffer _content;

    public File(Context context, int inodeNum, Inode inode) {
        __Context = context;
        __InodeNumber = inodeNum;
        __Inode = inode;
    }

    private Context __Context;

    protected Context getContext() {
        return __Context;
    }

    private Inode __Inode;

    public Inode getInode() {
        return __Inode;
    }

    private int __InodeNumber;

    public int getInodeNumber() {
        return __InodeNumber;
    }

    public long getLastAccessTimeUtc() {
        return getInode().AccessTime;
    }

    public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc() {
        return getInode().ModificationTime;
    }

    public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getCreationTimeUtc() {
        return getInode().CreationTime;
    }

    public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getFileAttributes() {
        return fromMode(getInode().Mode);
    }

    public void setFileAttributes(Map<String, Object> value) {
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

    private static Map<String, Object> fromMode(int mode) {
        return Utilities.fileAttributesFromUnixFileType(UnixFileType.valueOf((mode >> 12) & 0xF));
    }
}
