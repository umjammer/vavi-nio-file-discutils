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
import discUtils.core.vfs.IVfsFile;
import discUtils.streams.buffer.IBuffer;


public class File implements IVfsFile {

    protected final Context context;

    protected final Inode inode;

    private IBuffer content;

    public File(Context context, Inode inode) {
        this.context = context;
        this.inode = inode;
    }

    public long getLastAccessTimeUtc() {
        return inode.getAccessTime();
    }

    public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc() {
        return inode.getModificationTime();
    }

    public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getCreationTimeUtc() {
        return inode.getCreationTime();
    }

    public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        return UnixFileType.toFileAttributes(inode.getFileType());
    }

    public void setFileAttributes(EnumSet<FileAttributes> value) {
        throw new UnsupportedOperationException();
    }

    public long getFileLength() {
        return inode.getLength();
    }

    public IBuffer getFileContent() {
        if (content == null) {
            content = inode.getContentBuffer(context);
        }

        return content;
    }
}
