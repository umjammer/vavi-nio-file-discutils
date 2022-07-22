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

import discUtils.btrfs.base.DirItemChildType;
import discUtils.btrfs.base.Key;
import discUtils.streams.util.EndianUtilities;


/**
 * From an inode to a name in a directory
 */
public class DirItem extends BaseItem {

    public DirItem(Key key) {
        super(key);
    }

    /**
     * Key for the {@link InodeItem} or {@link RootItem} associated with this
     * entry. Unused and zeroed out when the entry describes an extended
     * attribute.
     */
    private Key childLocation;

    public Key getChildLocation() {
        return childLocation;
    }

    public void setChildLocation(Key value) {
        childLocation = value;
    }

    /**
     * transid
     */
    private long transId;

    public long getTransId() {
        return transId;
    }

    public void setTransId(long value) {
        transId = value;
    }

    /**
     * (m)
     */
    private short dataLength;

    public int getDataLength() {
        return dataLength & 0xffff;
    }

    public void setDataLength(short value) {
        dataLength = value;
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
     * type of child
     */
    private DirItemChildType childType = DirItemChildType.Unknown;

    public DirItemChildType getChildType() {
        return childType;
    }

    public void setChildType(DirItemChildType value) {
        childType = value;
    }

    /**
     * name of item in directory
     */
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    /**
     * data of item in directory (empty for normal directory items)
     */
    private byte[] data;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] value) {
        data = value;
    }

    public int size() {
        return 0x1e + getNameLength() + getDataLength();
    }

    public int readFrom(byte[] buffer, int offset) {
        childLocation = EndianUtilities.toStruct(Key.class, buffer, offset);
        transId = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x11);
        dataLength = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x19);
        nameLength = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x1b);
        childType = DirItemChildType.values()[buffer[offset + 0x1d]];
        name = new String(buffer, offset + 0x1e, getNameLength(), StandardCharsets.UTF_8);
        data = EndianUtilities.toByteArray(buffer, offset + 0x1e + getNameLength(), getDataLength());
        return size();
    }
}
