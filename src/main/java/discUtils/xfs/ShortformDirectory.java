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
import vavi.util.ByteUtil;


public class ShortformDirectory implements IByteArraySerializable {

    private final Context context;

    private boolean useShortInode;

    /**
     * Number of directory entries.
     */
    private byte count4Bytes;

    public int getCount4Bytes() {
        return count4Bytes & 0xff;
    }

    public void setCount4Bytes(byte value) {
        count4Bytes = value;
    }

    /**
     * Number of directory entries requiring 64-bit entries, if any inode
     * numbers require 64-bits. Zero otherwise.
     */
    private byte count8Bytes;

    public int getCount8Bytes() {
        return count8Bytes & 0xff;
    }

    public void setCount8Bytes(byte value) {
        count8Bytes = value;
    }

    private long parent;

    public long getParent() {
        return parent;
    }

    public void setParent(long value) {
        parent = value;
    }

    private ShortformDirectoryEntry[] entries;

    public ShortformDirectoryEntry[] getEntries() {
        return entries;
    }

    public void setEntries(ShortformDirectoryEntry[] value) {
        entries = value;
    }

    public ShortformDirectory(Context context) {
        this.context = context;
    }

    public int size() {
        int result = 0x6;
        for (ShortformDirectoryEntry entry : getEntries()) {
            result += entry.size();
        }
        return result;
    }

    public int readFrom(byte[] buffer, int offset) {
        count4Bytes = buffer[offset];
        count8Bytes = buffer[offset + 0x1];
        int count = getCount4Bytes();
        useShortInode = getCount8Bytes() == 0;
        offset += 0x2;
        if (useShortInode) {
            parent = ByteUtil.readBeInt(buffer, offset);
            offset += 0x4;
        } else {
            parent = ByteUtil.readBeLong(buffer, offset);
            offset += 0x8;
        }
        entries = new ShortformDirectoryEntry[count];
        for (int i = 0; i < count; i++) {
            ShortformDirectoryEntry entry = new ShortformDirectoryEntry(useShortInode, context);
            entry.readFrom(buffer, offset);
            offset += entry.size();
            entries[i] = entry;
        }
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
