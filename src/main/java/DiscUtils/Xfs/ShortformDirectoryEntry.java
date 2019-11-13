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

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class ShortformDirectoryEntry implements IByteArraySerializable, IDirectoryEntry {
    private final boolean _useShortInode;

    private final boolean _ftype;

    public ShortformDirectoryEntry(boolean useShortInode, Context context) {
        _useShortInode = useShortInode;
        _ftype = context.getSuperBlock().hasFType();
    }

    private byte _nameLength;

    public int getNameLength() {
        return _nameLength & 0xff;
    }

    public void setNameLength(byte value) {
        _nameLength = value;
    }

    private short _offset;

    public int getOffset() {
        return _offset & 0xffff;
    }

    public void setOffset(short value) {
        _offset = value;
    }

    private byte[] _name;

    public byte[] getName() {
        return _name;
    }

    public void setName(byte[] value) {
        _name = value;
    }

    private long _inode;

    public long getInode() {
        return _inode;
    }

    public void setInode(long value) {
        _inode = value;
    }

    private DirectoryFType _fType = DirectoryFType.File;

    public DirectoryFType getFType() {
        return _fType;
    }

    public void setFType(DirectoryFType value) {
        _fType = value;
    }

    public int size() {
        return 0x3 + getNameLength() + (_useShortInode ? 4 : 8) + (_ftype ? 1 : 0);
    }

    public int readFrom(byte[] buffer, int offset) {
        _nameLength = buffer[offset];
        _offset = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x1);
        _name = EndianUtilities.toByteArray(buffer, offset + 0x3, getNameLength());
        offset += 0x3 + getNameLength();
        if (_ftype) {
            _fType = DirectoryFType.values()[buffer[offset]];
            offset++;
        }

        if (_useShortInode) {
            setInode(EndianUtilities.toUInt32BigEndian(buffer, offset));
        } else {
            setInode(EndianUtilities.toUInt64BigEndian(buffer, offset));
        }
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     */
    public String toString() {
        return _inode + ": " + EndianUtilities.bytesToString(_name, 0, getNameLength());
    }
}
