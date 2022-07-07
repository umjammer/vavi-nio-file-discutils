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
    private Key _childLocation;

    public Key getChildLocation() {
        return _childLocation;
    }

    public void setChildLocation(Key value) {
        _childLocation = value;
    }

    /**
     * transid
     */
    private long _transId;

    public long getTransId() {
        return _transId;
    }

    public void setTransId(long value) {
        _transId = value;
    }

    /**
     * (m)
     */
    private short _dataLength;

    public int getDataLength() {
        return _dataLength & 0xffff;
    }

    public void setDataLength(short value) {
        _dataLength = value;
    }

    /**
     * (n)
     */
    private short _nameLength;

    public int getNameLength() {
        return _nameLength & 0xffff;
    }

    public void setNameLength(short value) {
        _nameLength = value;
    }

    /**
     * type of child
     */
    private DirItemChildType _childType = DirItemChildType.Unknown;

    public DirItemChildType getChildType() {
        return _childType;
    }

    public void setChildType(DirItemChildType value) {
        _childType = value;
    }

    /**
     * name of item in directory
     */
    private String _name;

    public String getName() {
        return _name;
    }

    public void setName(String value) {
        _name = value;
    }

    /**
     * data of item in directory (empty for normal directory items)
     */
    private byte[] _data;

    public byte[] getData() {
        return _data;
    }

    public void setData(byte[] value) {
        _data = value;
    }

    public int size() {
        return 0x1e + getNameLength() + getDataLength();
    }

    public int readFrom(byte[] buffer, int offset) {
        setChildLocation(EndianUtilities.toStruct(Key.class, buffer, offset));
        setTransId(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x11));
        setDataLength(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x19));
        setNameLength(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x1b));
        setChildType(DirItemChildType.values()[buffer[offset + 0x1d]]);
        setName(new String(buffer, offset + 0x1e, getNameLength(), StandardCharsets.UTF_8));
        setData(EndianUtilities.toByteArray(buffer, offset + 0x1e + getNameLength(), getDataLength()));
        return size();
    }
}
