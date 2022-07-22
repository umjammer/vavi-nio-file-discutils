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

import discUtils.streams.util.EndianUtilities;


public class RegularInode extends Inode {

    private int fileSize;

    public int fragmentKey;

    public int fragmentOffset;

    public int startBlock;

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long value) {
        if (value > 0xffff_ffffL) {
            throw new IndexOutOfBoundsException("File size greater than " + 0xffff_ffffL);
        }

        fileSize = (int) value;
    }

    public int size() {
        return 32;
    }

    public int readFrom(byte[] buffer, int offset) {
        super.readFrom(buffer, offset);
        numLinks = 1;
        startBlock = EndianUtilities.toUInt32LittleEndian(buffer, offset + 16);
        fragmentKey = EndianUtilities.toUInt32LittleEndian(buffer, offset + 20);
        fragmentOffset = EndianUtilities.toUInt32LittleEndian(buffer, offset + 24);
        fileSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 28);
        return 32;
    }

    public void writeTo(byte[] buffer, int offset) {
        super.writeTo(buffer, offset);
        EndianUtilities.writeBytesLittleEndian(startBlock, buffer, offset + 16);
        EndianUtilities.writeBytesLittleEndian(fragmentKey, buffer, offset + 20);
        EndianUtilities.writeBytesLittleEndian(fragmentOffset, buffer, offset + 24);
        EndianUtilities.writeBytesLittleEndian(fileSize, buffer, offset + 28);
    }
}
