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

    private long _inode;

    public long getInode() {
        return _inode;
    }

    public void setInode(long value) {
        _inode = value;
    }

    private byte _nameLength;

    public int getNameLength() {
        return _nameLength & 0xff;
    }

    public void setNameLength(byte value) {
        _nameLength = value;
    }

    private byte[] _name;

    public byte[] getName() {
        return _name;
    }

    public void setName(byte[] value) {
        _name = value;
    }

    private short _tag;

    public short getTag() {
        return _tag;
    }

    public void setTag(short value) {
        _tag = value;
    }

    private DirectoryFType _fType = DirectoryFType.File;

    public DirectoryFType getFType() {
        return _fType;
    }

    public void setFType(DirectoryFType value) {
        _fType = value;
    }

    public int size() {
        int size = 0xb + getNameLength() + (_ftype ? 1 : 0);
        int padding = size % 8;
        if (padding != 0)
            return size + (8 - padding);

        return size;
    }

    public BlockDirectoryDataEntry(Context context) {
        _ftype = context.getSuperBlock().hasFType();
    }

    public int readFrom(byte[] buffer, int offset) {
        setInode(EndianUtilities.toUInt64BigEndian(buffer, offset));
        setNameLength(buffer[offset + 0x8]);
        setName(EndianUtilities.toByteArray(buffer, offset + 0x9, getNameLength()));
        offset += 0x9 + getNameLength();
        if (_ftype) {
            setFType(DirectoryFType.values()[buffer[offset]]);
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
        return String.format("%d : %s", _inode, EndianUtilities.bytesToString(_name, 0, _nameLength));
    }
}
