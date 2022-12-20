//
// Copyright (c) 2016-2017, Bianco Veigel
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

import discUtils.core.vfs.IVfsSymlink;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.IOException;


public class Symlink extends File implements IVfsSymlink<DirEntry, File> {
    public Symlink(Context context, Inode inode) {
        super(context, inode);
    }

    @Override
    public String getTargetPath() {
        if (inode.getFormat() != InodeFormat.Local && inode.getFormat() != InodeFormat.Extents) {
            throw new IOException("invalid Inode format for symlink");
        }

        IBuffer content = getFileContent();
        byte[] data = StreamUtilities.readExact(content, 0, (int) inode.getLength());
        return new String(data, 0, data.length, context.getOptions().getFileNameEncoding()).replace('/', java.io.File.separatorChar);
    }
}
