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

import java.util.ArrayList;
import java.util.List;

import discUtils.core.vfs.IVfsDirectory;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.StreamUtilities;


public class Directory extends File implements IVfsDirectory<DirEntry, File> {

    public Directory(Context context, int inodeNum, Inode inode) {
        super(context, inodeNum, inode);
    }

    @Override  public List<DirEntry> getAllEntries() {
        List<DirEntry> dirEntries = new ArrayList<>();
        IBuffer content = getFileContent();
        int blockSize = getContext().getSuperBlock().getBlockSize();
        byte[] blockData = new byte[blockSize];
        int relBlock = 0;
        long pos = 0;
        while (pos < getInode().fileSize) {
            StreamUtilities.readMaximum(content, blockSize * (long) relBlock, blockData, 0, blockSize);
            int blockPos = 0;
            while (blockPos < blockSize) {
                DirectoryRecord r = new DirectoryRecord(getContext().getOptions().getFileNameEncoding());
                int numRead = r.readFrom(blockData, blockPos);
                if (r.inode != 0 && !r.name.equals(".") && !r.name.equals("..")) {
                    dirEntries.add(new DirEntry(r));
                }

                blockPos += numRead;
            }
            ++relBlock;
            pos += blockSize;
        }
        return dirEntries;
    }

    @Override public DirEntry getSelf() {
        return null;
    }

    @Override public DirEntry getEntryByName(String name) {
        for (DirEntry entry : getAllEntries()) {
            if (entry.getFileName().equals(name)) {
                return entry;
            }
        }
        return null;
    }

    @Override public DirEntry createNewFile(String name) {
        throw new UnsupportedOperationException();
    }
}
