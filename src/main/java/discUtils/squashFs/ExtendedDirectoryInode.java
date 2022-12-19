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


public class ExtendedDirectoryInode extends Inode implements IDirectoryInode {

    @SuppressWarnings("unused")
    private int extendedAttributes;

    private int fileSize;

    @SuppressWarnings("unused")
    private short indexCount;

    public int size() {
        return 40;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long value) {
        if (value > 0xffff_ffffL) {
            throw new IndexOutOfBoundsException("File size greater than " + 0xffff_ffffL);
        }

        fileSize = (int) value;
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
        numLinks = ByteUtil.readLeInt(buffer, offset + 16);
        fileSize = ByteUtil.readLeInt(buffer, offset + 20);
        startBlock = ByteUtil.readLeInt(buffer, offset + 24);
        parentInode = ByteUtil.readLeInt(buffer, offset + 28);
        indexCount = ByteUtil.readLeShort(buffer, offset + 32);
        this.offset = ByteUtil.readLeShort(buffer, offset + 34);
        extendedAttributes = ByteUtil.readLeInt(buffer, offset + 36);
        return 40;
    }
}
