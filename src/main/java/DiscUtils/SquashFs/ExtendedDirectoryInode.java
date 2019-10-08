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

package DiscUtils.SquashFs;

import DiscUtils.Streams.Util.EndianUtilities;


public class ExtendedDirectoryInode extends Inode implements IDirectoryInode {
    private int _extendedAttributes;

    private int _fileSize;

    private short _indexCount;

    public long getSize() {
        return 40;
    }

    public long getFileSize() {
        return _fileSize;
    }

    public void setFileSize(long value) {
        if (value > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("File size greater than " + Integer.MAX_VALUE);
        }

        _fileSize = (int) value;
    }

    private int __StartBlock;

    public int getStartBlock() {
        return __StartBlock;
    }

    public void setStartBlock(int value) {
        __StartBlock = value;
    }

    private int __ParentInode;

    public int getParentInode() {
        return __ParentInode;
    }

    public void setParentInode(int value) {
        __ParentInode = value;
    }

    private short __Offset;

    public short getOffset() {
        return __Offset;
    }

    public void setOffset(short value) {
        __Offset = value;
    }

    public int readFrom(byte[] buffer, int offset) {
        super.readFrom(buffer, offset);
        NumLinks = EndianUtilities.toInt32LittleEndian(buffer, offset + 16);
        _fileSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 20);
        setStartBlock(EndianUtilities.toUInt32LittleEndian(buffer, offset + 24));
        setParentInode(EndianUtilities.toUInt32LittleEndian(buffer, offset + 28));
        _indexCount = (short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 32);
        setOffset((short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 34));
        _extendedAttributes = EndianUtilities.toUInt32LittleEndian(buffer, offset + 36);
        return 40;
    }
}