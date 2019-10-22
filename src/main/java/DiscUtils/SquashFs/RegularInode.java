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


public class RegularInode extends Inode {
    private int _fileSize;

    public int FragmentKey;

    public int FragmentOffset;

    public int StartBlock;

    public long getFileSize() {
        return _fileSize;
    }

    public void setFileSize(long value) {
        if (value > 0xffffffffl) {
            throw new IndexOutOfBoundsException("File size greater than " + 0xffffffffl);
        }

        _fileSize = (int) value;
    }

    public long getSize() {
        return 32;
    }

    public int readFrom(byte[] buffer, int offset) {
        super.readFrom(buffer, offset);
        NumLinks = 1;
        StartBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 16);
        FragmentKey = EndianUtilities.toUInt32LittleEndian(buffer, offset + 20);
        FragmentOffset = EndianUtilities.toUInt32LittleEndian(buffer, offset + 24);
        _fileSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 28);
        return 32;
    }

    public void writeTo(byte[] buffer, int offset) {
        super.writeTo(buffer, offset);
        EndianUtilities.writeBytesLittleEndian(StartBlock, buffer, offset + 16);
        EndianUtilities.writeBytesLittleEndian(FragmentKey, buffer, offset + 20);
        EndianUtilities.writeBytesLittleEndian(FragmentOffset, buffer, offset + 24);
        EndianUtilities.writeBytesLittleEndian(_fileSize, buffer, offset + 28);
    }
}
