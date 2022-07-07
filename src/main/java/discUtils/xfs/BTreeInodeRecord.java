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
    private int __StartInode;

    public int getStartInode() {
        return __StartInode;
    }

    public void setStartInode(int value) {
        __StartInode = value;
    }

    /**
     * specifies the number of free entries in the chuck
     */
    private int __FreeCount;

    public int getFreeCount() {
        return __FreeCount;
    }

    public void setFreeCount(int value) {
        __FreeCount = value;
    }

    /**
     * 64 element bit array specifying which entries are free in the chunk
     */
    private BitSet __Free;

    public BitSet getFree() {
        return __Free;
    }

    public void setFree(BitSet value) {
        __Free = value;
    }

    public int size() {
        return 0x10;
    }

    public int readFrom(byte[] buffer, int offset) {
        setStartInode(EndianUtilities.toUInt32BigEndian(buffer, offset));
        setFreeCount(EndianUtilities.toUInt32BigEndian(buffer, offset + 0x4));
        setFree(BitSet.valueOf(EndianUtilities.toByteArray(buffer, offset + 0x8, 0x8)));
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
