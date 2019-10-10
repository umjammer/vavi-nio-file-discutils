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

import DiscUtils.Btrfs.Base.DirItemChildType;
import DiscUtils.Btrfs.Base.Key;
import DiscUtils.Streams.Util.EndianUtilities;


/**
 * From an inode to a name in a directory
 */
public class DirItem extends BaseItem {
    public DirItem(Key key) {
        super(key);
    }

    /**
     * Key for the
     * {@link InodeItem}
     * or
     * {@link RootItem}
     * associated with this entry.
     * Unused and zeroed out when the entry describes an extended attribute.
     */
    private Key __ChildLocation;

    public Key getChildLocation() {
        return __ChildLocation;
    }

    public void setChildLocation(Key value) {
        __ChildLocation = value;
    }

    /**
     * transid
     */
    private long __TransId;

    public long getTransId() {
        return __TransId;
    }

    public void setTransId(long value) {
        __TransId = value;
    }

    /**
     * (m)
     */
    private short __DataLength;

    public short getDataLength() {
        return __DataLength;
    }

    public void setDataLength(short value) {
        __DataLength = value;
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
     * type of child
     */
    private DirItemChildType __ChildType = DirItemChildType.Unknown;

    public DirItemChildType getChildType() {
        return __ChildType;
    }

    public void setChildType(DirItemChildType value) {
        __ChildType = value;
    }

    /**
     * name of item in directory
     */
    private String __Name;

    public String getName() {
        return __Name;
    }

    public void setName(String value) {
        __Name = value;
    }

    /**
     * data of item in directory (empty for normal directory items)
     */
    private byte[] __Data;

    public byte[] getData() {
        return __Data;
    }

    public void setData(byte[] value) {
        __Data = value;
    }

    public long getSize() {
        return 0x1e + getNameLength() + getDataLength();
    }

    public int readFrom(byte[] buffer, int offset) {
        setChildLocation(EndianUtilities.<Key> toStruct(Key.class, buffer, offset));
        setTransId(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x11));
        setDataLength((short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x19));
        setNameLength((short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x1b));
        setChildType(DirItemChildType.valueOf(buffer[offset + 0x1d]));
        setName(new String(buffer, offset + 0x1e, getNameLength(), Charset.forName("UTF8")));
        setData(EndianUtilities.toByteArray(buffer, offset + 0x1e + getNameLength(), getDataLength()));
        return (int) getSize();
    }
}
