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
    @SuppressWarnings("unused")
    private int _extendedAttributes;

    private int _fileSize;

    @SuppressWarnings("unused")
    private short _indexCount;

    public int size() {
        return 40;
    }

    public long getFileSize() {
        return _fileSize;
    }

    public void setFileSize(long value) {
        if (value > 0xffff_ffffl) {
            throw new IndexOutOfBoundsException("File size greater than " + 0xffff_ffffl);
        }

        _fileSize = (int) value;
    }

    private int _startBlock;

    public int getStartBlock() {
        return _startBlock;
    }

    public void setStartBlock(int value) {
        _startBlock = value;
    }

    private int _parentInode;

    public int getParentInode() {
        return _parentInode;
    }

    public void setParentInode(int value) {
        _parentInode = value;
    }

    private short _offset;

    public int getOffset() {
        return _offset & 0xffff;
    }

    public void setOffset(short value) {
        _offset = value;
    }

    public int readFrom(byte[] buffer, int offset) {
        super.readFrom(buffer, offset);
        _numLinks = EndianUtilities.toInt32LittleEndian(buffer, offset + 16);
        _fileSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 20);
        _startBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 24);
        _parentInode = EndianUtilities.toUInt32LittleEndian(buffer, offset + 28);
        _indexCount = EndianUtilities.toUInt16LittleEndian(buffer, offset + 32);
        _offset = EndianUtilities.toUInt16LittleEndian(buffer, offset + 34);
        _extendedAttributes = EndianUtilities.toUInt32LittleEndian(buffer, offset + 36);
        return 40;
    }
}
