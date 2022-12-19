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

import vavi.util.ByteUtil;


public class DirectoryInode extends Inode implements IDirectoryInode {

    private short fileSize;

    public int size() {
        return 32;
    }

    public long getFileSize() {
        return fileSize & 0xffff;
    }

    public void setFileSize(long value) {
        if (value > 0xffff) {
            throw new IndexOutOfBoundsException("File size greater than " + 0xffff);
        }

        fileSize = (short) value;
    }

    private int startBlock;

    public int getStartBlock() {
        return startBlock;
    }

    public void setStartBlock(int value) {
        startBlock = value;
    }

    private int parentInode;

    public int getParentInode() {
        return parentInode;
    }

    public void setParentInode(int value) {
        parentInode = value;
    }

    private short offset;

    public int getOffset() {
        return offset & 0xffff;
    }

    public void setOffset(short value) {
        offset = value;
    }

    public int readFrom(byte[] buffer, int offset) {
        super.readFrom(buffer, offset);

        startBlock = ByteUtil.readLeInt(buffer, offset + 16);
        numLinks = ByteUtil.readLeInt(buffer, offset + 20);
        fileSize = ByteUtil.readLeShort(buffer, offset + 24);
        this.offset = ByteUtil.readLeShort(buffer, offset + 26);
        parentInode = ByteUtil.readLeInt(buffer, offset + 28);

        return 32;
    }

    public void writeTo(byte[] buffer, int offset) {
        super.writeTo(buffer, offset);

        ByteUtil.writeLeInt(startBlock, buffer, offset + 16);
        ByteUtil.writeLeInt(numLinks, buffer, offset + 20);
        ByteUtil.writeLeShort(fileSize, buffer, offset + 24);
        ByteUtil.writeLeShort(this.offset, buffer, offset + 26);
        ByteUtil.writeLeInt(parentInode, buffer, offset + 28);
    }
}
