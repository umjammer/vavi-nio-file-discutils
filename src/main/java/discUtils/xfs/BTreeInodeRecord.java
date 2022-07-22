//
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

import java.util.BitSet;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public class BTreeInodeRecord implements IByteArraySerializable {

    /**
     * specifies the starting inode number for the chunk
     */
    private int startInode;

    public int getStartInode() {
        return startInode;
    }

    public void setStartInode(int value) {
        startInode = value;
    }

    /**
     * specifies the number of free entries in the chuck
     */
    private int freeCount;

    public int getFreeCount() {
        return freeCount;
    }

    public void setFreeCount(int value) {
        freeCount = value;
    }

    /**
     * 64 element bit array specifying which entries are free in the chunk
     */
    private BitSet free;

    public BitSet getFree() {
        return free;
    }

    public void setFree(BitSet value) {
        free = value;
    }

    public int size() {
        return 0x10;
    }

    public int readFrom(byte[] buffer, int offset) {
        startInode = EndianUtilities.toUInt32BigEndian(buffer, offset);
        freeCount = EndianUtilities.toUInt32BigEndian(buffer, offset + 0x4);
        free = BitSet.valueOf(EndianUtilities.toByteArray(buffer, offset + 0x8, 0x8));
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
