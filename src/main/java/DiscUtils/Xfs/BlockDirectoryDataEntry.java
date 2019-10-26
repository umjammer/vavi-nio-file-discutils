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

package DiscUtils.Xfs;

import DiscUtils.Streams.Util.EndianUtilities;


public class BlockDirectoryDataEntry extends BlockDirectoryData implements IDirectoryEntry {
    private final boolean _ftype;

    private long __Inode;

    public long getInode() {
        return __Inode;
    }

    public void setInode(long value) {
        __Inode = value;
    }

    private byte __NameLength;

    public byte getNameLength() {
        return __NameLength;
    }

    public void setNameLength(byte value) {
        __NameLength = value;
    }

    private byte[] __Name;

    public byte[] getName() {
        return __Name;
    }

    public void setName(byte[] value) {
        __Name = value;
    }

    private short __Tag;

    public short getTag() {
        return __Tag;
    }

    public void setTag(short value) {
        __Tag = value;
    }

    private DirectoryFType __FType = DirectoryFType.File;

    public DirectoryFType getFType() {
        return __FType;
    }

    public void setFType(DirectoryFType value) {
        __FType = value;
    }

    public int size() {
        int size = 0xb + getNameLength() + (_ftype ? 1 : 0);
        int padding = size % 8;
        if (padding != 0)
            return size + (8 - padding);

        return size;
    }

    public BlockDirectoryDataEntry(Context context) {
        _ftype = context.getSuperBlock().getHasFType();
    }

    public int readFrom(byte[] buffer, int offset) {
        setInode(EndianUtilities.toUInt64BigEndian(buffer, offset));
        setNameLength(buffer[offset + 0x8]);
        setName(EndianUtilities.toByteArray(buffer, offset + 0x9, getNameLength()));
        offset += 0x9 + getNameLength();
        if (_ftype) {
            setFType(DirectoryFType.valueOf(buffer[offset]));
            offset++;
        }

        int padding = 6 - ((getNameLength() + (_ftype ? 2 : 1)) % 8);
        if (padding < 0)
            padding += 8;

        offset += padding;
        setTag(EndianUtilities.toUInt16BigEndian(buffer, offset));
        return size();
    }

    /**
     *
     */
    public String toString() {
        return String.format("%d : %s", __Inode, EndianUtilities.bytesToString(__Name, 0, __NameLength));
    }
}
