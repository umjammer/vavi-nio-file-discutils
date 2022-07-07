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

package discUtils.btrfs.base;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public class Key implements IByteArraySerializable {
    public static final int Length = 0x11;

    public Key() {
    }

    public Key(long objectId, ItemType type, long offset) {
        this();
        _objectId = objectId;
        _itemType = type;
        _offset = offset;
    }

    public Key(long objectId, ItemType type) {
        this(objectId, type, 0L);
    }

    public Key(ReservedObjectId objectId, ItemType type) {
        this(objectId.getValue(), type);
    }

    /**
     * Object ID. Each tree has its own set of Object IDs.
     */
    private long _objectId;

    public long getObjectId() {
        return _objectId;
    }

    public void setObjectId(long value) {
        _objectId = value;
    }

    public ReservedObjectId getReservedObjectId() {
        return ReservedObjectId.valueOf((int) getObjectId());
    }

    /**
     * Item type.
     */
    private ItemType _itemType = ItemType.InodeItem;

    public ItemType getItemType() {
        return _itemType;
    }

    public void setItemType(ItemType value) {
        _itemType = value;
    }

    /**
     * Offset. The meaning depends on the item type.
     */
    private long _offset;

    public long getOffset() {
        return _offset;
    }

    public void setOffset(long value) {
        _offset = value;
    }

    public int size() {
        return Length;
    }

    public int readFrom(byte[] buffer, int offset) {
        _objectId = EndianUtilities.toUInt64LittleEndian(buffer, offset);
        _itemType = ItemType.valueOf(buffer[offset + 0x8] & 0xff);
        _offset = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x9);
        return size();
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        try {
            ReservedObjectId.valueOf((int) getObjectId());
            return String.format("%d|%s|%d", _objectId, _itemType, _offset);
        } catch (Exception e) {
            return String.format("%d|%s|%d", _objectId, _itemType, _offset);
        }
    }
}
