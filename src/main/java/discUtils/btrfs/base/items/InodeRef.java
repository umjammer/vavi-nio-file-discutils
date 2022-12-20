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
public class InodeRef extends BaseItem {

    public InodeRef(Key key) {
        super(key);
    }

    /**
     * index in the directory
     */
    private long index;

    public long getIndex() {
        return index;
    }

    public void setIndex(long value) {
        index = value;
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
     * name in the directory
     */
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    @Override public int size() {
        return 0xa + getNameLength();
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        index = ByteUtil.readLeLong(buffer, offset);
        nameLength = ByteUtil.readLeShort(buffer, offset + 0x8);
        name = new String(buffer, offset + 0xa, getNameLength(), StandardCharsets.UTF_8);
        return size();
    }
}
