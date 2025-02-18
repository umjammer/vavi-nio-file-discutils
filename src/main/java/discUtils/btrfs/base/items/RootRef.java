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

package discUtils.btrfs.base.items;

import java.nio.charset.StandardCharsets;

import discUtils.btrfs.base.Key;
import vavi.util.ByteUtil;


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
    private long directoryId;

    public long getDirectoryId() {
        return directoryId;
    }

    public void setDirectoryId(long value) {
        directoryId = value;
    }

    /**
     * Sequence (index in tree) (even, starting at 2?)
     */
    private long sequence;

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long value) {
        sequence = value;
    }

    /**
     * (n)
     */
    private short nameLength;

    public int getNameLength() {
        return nameLength & 0xffff;
    }

    public void setNameLength(short value) {
        nameLength = value;
    }

    /**
     * name
     */
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    @Override public int size() {
        return 0x12 + getNameLength();
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        setDirectoryId(ByteUtil.readLeLong(buffer, offset));
        setSequence(ByteUtil.readLeLong(buffer, offset + 0x8));
        setNameLength(ByteUtil.readLeShort(buffer, offset + 0x10));
        setName(new String(buffer, offset + 0x12, getNameLength(), StandardCharsets.UTF_8));
        return size();
    }
}
