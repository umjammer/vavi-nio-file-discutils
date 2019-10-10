//
// Copyright (c) 2017, Bianco Veigel
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

package DiscUtils.Btrfs.Base.Items;

import java.nio.charset.Charset;

import DiscUtils.Btrfs.Base.Key;
import DiscUtils.Streams.Util.EndianUtilities;


/**
 * From an inode to a name in a directory
 */
public class RootRef extends BaseItem {
    public RootRef(Key key) {
        super(key);
    }

    /**
     * ID of directory in [tree id] that contains the subtree
     */
    private long __DirectoryId;

    public long getDirectoryId() {
        return __DirectoryId;
    }

    public void setDirectoryId(long value) {
        __DirectoryId = value;
    }

    /**
     * Sequence (index in tree) (even, starting at 2?)
     */
    private long __Sequence;

    public long getSequence() {
        return __Sequence;
    }

    public void setSequence(long value) {
        __Sequence = value;
    }

    /**
     * (n)
     */
    private short __NameLength;

    public short getNameLength() {
        return __NameLength;
    }

    public void setNameLength(short value) {
        __NameLength = value;
    }

    /**
     * name
     */
    private String __Name = new String();

    public String getName() {
        return __Name;
    }

    public void setName(String value) {
        __Name = value;
    }

    public long getSize() {
        return 0x12 + getNameLength();
    }

    public int readFrom(byte[] buffer, int offset) {
        setDirectoryId(EndianUtilities.toUInt64LittleEndian(buffer, offset));
        setSequence(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x8));
        setNameLength((short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x10));
        setName(new String(buffer, offset + 0x12, getNameLength(), Charset.forName("UTF8")));
        return (int) getSize();
    }
}
