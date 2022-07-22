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

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public class ShortformDirectoryEntry implements IByteArraySerializable, IDirectoryEntry {

    private final boolean useShortInode;

    private final boolean ftype;

    public ShortformDirectoryEntry(boolean useShortInode, Context context) {
        this.useShortInode = useShortInode;
        ftype = context.getSuperBlock().hasFType();
    }

    private byte nameLength;

    public int getNameLength() {
        return nameLength & 0xff;
    }

    public void setNameLength(byte value) {
        nameLength = value;
    }

    private short offset;

    public int getOffset() {
        return offset & 0xffff;
    }

    public void setOffset(short value) {
        offset = value;
    }

    private byte[] name;

    public byte[] getName() {
        return name;
    }

    public void setName(byte[] value) {
        name = value;
    }

    private long inode;

    public long getInode() {
        return inode;
    }

    public void setInode(long value) {
        inode = value;
    }

    private DirectoryFType fType = DirectoryFType.File;

    public DirectoryFType getFType() {
        return fType;
    }

    public void setFType(DirectoryFType value) {
        fType = value;
    }

    public int size() {
        return 0x3 + getNameLength() + (useShortInode ? 4 : 8) + (ftype ? 1 : 0);
    }

    public int readFrom(byte[] buffer, int offset) {
        nameLength = buffer[offset];
        this.offset = EndianUtilities.toUInt16BigEndian(buffer, offset + 0x1);
        name = EndianUtilities.toByteArray(buffer, offset + 0x3, getNameLength());
        offset += 0x3 + getNameLength();
        if (ftype) {
            fType = DirectoryFType.values()[buffer[offset]];
            offset++;
        }

        if (useShortInode) {
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
        return inode + ": " + EndianUtilities.bytesToString(name, 0, getNameLength());
    }
}
