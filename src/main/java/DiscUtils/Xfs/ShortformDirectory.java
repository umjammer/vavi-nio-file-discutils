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


public class ShortformDirectory implements IByteArraySerializable {
    private final Context _context;

    private boolean _useShortInode;

    /**
     * Number of directory entries.
     */
    private byte __Count4Bytes;

    public byte getCount4Bytes() {
        return __Count4Bytes;
    }

    public void setCount4Bytes(byte value) {
        __Count4Bytes = value;
    }

    /**
     * Number of directory entries requiring 64-bit entries, if any inode
     * numbers require 64-bits. Zero otherwise.
     */
    private byte __Count8Bytes;

    public byte getCount8Bytes() {
        return __Count8Bytes;
    }

    public void setCount8Bytes(byte value) {
        __Count8Bytes = value;
    }

    private long __Parent;

    public long getParent() {
        return __Parent;
    }

    public void setParent(long value) {
        __Parent = value;
    }

    private ShortformDirectoryEntry[] __Entries;

    public ShortformDirectoryEntry[] getEntries() {
        return __Entries;
    }

    public void setEntries(ShortformDirectoryEntry[] value) {
        __Entries = value;
    }

    public ShortformDirectory(Context context) {
        _context = context;
    }

    public long getSize() {
        int result = 0x6;
        for (ShortformDirectoryEntry entry : getEntries()) {
            result += entry.getSize();
        }
        return result;
    }

    public int readFrom(byte[] buffer, int offset) {
        setCount4Bytes(buffer[offset]);
        setCount8Bytes(buffer[offset + 0x1]);
        byte count = getCount4Bytes();
        _useShortInode = getCount8Bytes() == 0;
        offset += 0x2;
        if (_useShortInode) {
            setParent(EndianUtilities.toUInt32BigEndian(buffer, offset));
            offset += 0x4;
        } else {
            setParent(EndianUtilities.toUInt64BigEndian(buffer, offset));
            offset += 0x8;
        }
        setEntries(new ShortformDirectoryEntry[count]);
        for (int i = 0; i < count; i++) {
            ShortformDirectoryEntry entry = new ShortformDirectoryEntry(_useShortInode, _context);
            entry.readFrom(buffer, offset);
            offset += entry.getSize();
            getEntries()[i] = entry;
        }
        return (int) getSize();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
