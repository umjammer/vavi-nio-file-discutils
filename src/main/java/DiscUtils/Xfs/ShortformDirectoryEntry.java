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
        _ftype = context.getSuperBlock().getHasFType();
    }

    private byte __NameLength;

    public byte getNameLength() {
        return __NameLength;
    }

    public void setNameLength(byte value) {
        __NameLength = value;
    }

    private short __Offset;

    public short getOffset() {
        return __Offset;
    }

    public void setOffset(short value) {
        __Offset = value;
    }

    private byte[] __Name;

    public byte[] getName() {
        return __Name;
    }

    public void setName(byte[] value) {
        __Name = value;
    }

    private long __Inode;

    public long getInode() {
        return __Inode;
    }

    public void setInode(long value) {
        __Inode = value;
    }

    private DirectoryFType __FType = DirectoryFType.File;

    public DirectoryFType getFType() {
        return __FType;
    }

    public void setFType(DirectoryFType value) {
        __FType = value;
    }

    public int sizeOf() {
        return 0x3 + getNameLength() + (_useShortInode ? 4 : 8) + (_ftype ? 1 : 0);
    }

    public int readFrom(byte[] buffer, int offset) {
        setNameLength(buffer[offset]);
        setOffset(EndianUtilities.toUInt16BigEndian(buffer, offset + 0x1));
        setName(EndianUtilities.toByteArray(buffer, offset + 0x3, getNameLength()));
        offset += 0x3 + getNameLength();
        if (_ftype) {
            setFType(DirectoryFType.valueOf(buffer[offset]));
            offset++;
        }

        if (_useShortInode) {
            setInode(EndianUtilities.toUInt32BigEndian(buffer, offset));
        } else {
            setInode(EndianUtilities.toUInt64BigEndian(buffer, offset));
        }
        return sizeOf();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     */
    public String toString() {
        return __Inode + ": " + EndianUtilities.bytesToString(__Name, 0, __NameLength);
    }
}
